package com.attafitamim.kabin.compiler.sql.generator.dao

import com.attafitamim.kabin.compiler.sql.generator.references.FunctionReference
import com.attafitamim.kabin.compiler.sql.generator.references.ParameterReference
import com.attafitamim.kabin.compiler.sql.syntax.SQLQuery
import com.attafitamim.kabin.compiler.sql.utils.poet.SYMBOL_ACCESS_SIGN
import com.attafitamim.kabin.compiler.sql.utils.poet.buildSpec
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.asName
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.getColumnAccessChain
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.getParametersCall
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.isNullableAccess
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.toParameterAccess
import com.attafitamim.kabin.compiler.sql.utils.poet.simpleNameString
import com.attafitamim.kabin.compiler.sql.utils.poet.typeInitializer
import com.attafitamim.kabin.compiler.sql.utils.poet.writeType
import com.attafitamim.kabin.compiler.sql.utils.spec.getEntityDataType
import com.attafitamim.kabin.compiler.sql.utils.spec.getNestedDataType
import com.attafitamim.kabin.compiler.sql.utils.spec.getQueryByColumnsName
import com.attafitamim.kabin.compiler.sql.utils.spec.getQueryFunctionName
import com.attafitamim.kabin.compiler.sql.utils.spec.toSortedSet
import com.attafitamim.kabin.compiler.sql.utils.sql.dao.getParameterReferences
import com.attafitamim.kabin.compiler.sql.utils.sql.dao.getSQLQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.dao.getSelectSQLQuery
import com.attafitamim.kabin.core.dao.KabinDao
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.dao.DaoActionSpec
import com.attafitamim.kabin.specs.dao.DaoFunctionSpec
import com.attafitamim.kabin.specs.dao.DaoSpec
import com.attafitamim.kabin.specs.dao.DataTypeSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.attafitamim.kabin.specs.relation.compound.CompoundPropertySpec
import com.attafitamim.kabin.specs.relation.compound.CompoundRelationSpec
import com.attafitamim.kabin.specs.relation.compound.CompoundSpec
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

class DaoGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: KabinOptions
) {

    private val daoQueriesPropertyName = KabinDao<*>::queries.name

    fun generate(daoSpec: DaoSpec): Result {
        val daoFilePackage = daoSpec.declaration.packageName.asString()
        val daoFileName = buildString {
            append(daoSpec.declaration.simpleName.asString())
            append(options.getOrDefault(KabinOptions.Key.DAO_SUFFIX))
        }

        val daoQueriesFilePackage = daoSpec.declaration.packageName.asString()
        val daoQueriesFileName = buildString {
            append(daoSpec.declaration.simpleName.asString())
            append(options.getOrDefault(KabinOptions.Key.DAO_QUERIES_SUFFIX))
        }

        val className = ClassName(daoFilePackage, daoFileName)
        val daoQueriesClassName = ClassName(daoQueriesFilePackage, daoQueriesFileName)

        val superClassName = daoSpec.declaration.toClassName()
        val kabinDaoInterface = KabinDao::class.asClassName()
            .parameterizedBy(daoQueriesClassName)

        val classBuilder = TypeSpec.classBuilder(className)
            .addSuperinterface(kabinDaoInterface)
            .addSuperinterface(superClassName)

        val constructorBuilder = FunSpec.constructorBuilder()
            .addParameter(daoQueriesPropertyName, daoQueriesClassName)
            .build()

        val daoQueriesPropertySpec = PropertySpec.builder(
            daoQueriesPropertyName,
            daoQueriesClassName,
            KModifier.OVERRIDE
        ).initializer(daoQueriesPropertyName).build()

        classBuilder
            .primaryConstructor(constructorBuilder)
            .addProperty(daoQueriesPropertySpec)

        val addedFunctions = HashSet<FunctionReference>()
        daoSpec.functionSpecs.forEach { functionSpec ->
            val isTransaction = functionSpec.transactionSpec != null
            val actionSpec = functionSpec.actionSpec
            if (actionSpec == null && !isTransaction) {
                return@forEach
            }

            val functionCodeBuilder = CodeBlock.builder()
            val returnType = functionSpec.returnTypeSpec
            if (isTransaction) {
                if (returnType != null) {
                    functionCodeBuilder.beginControlFlow("return·transactionWithResult")
                } else {
                    functionCodeBuilder.beginControlFlow("transaction")
                }
            }

            functionCodeBuilder.addReturnLogic(
                classBuilder,
                addedFunctions,
                functionSpec,
                returnType,
                isTransaction
            )

            if (functionSpec.transactionSpec != null) {
                functionCodeBuilder.endControlFlow()
            }

            val functionBuilder = functionSpec.declaration.buildSpec()
                .addModifiers(KModifier.OVERRIDE)
                .addCode(functionCodeBuilder.build())
                .build()

            classBuilder.addFunction(functionBuilder)
        }

        codeGenerator.writeType(
            className,
            classBuilder.build()
        )

        return Result(className)
    }

    private fun CodeBlock.Builder.addReturnLogic(
        daoBuilder: TypeSpec.Builder,
        addedFunctions: MutableSet<FunctionReference>,
        functionSpec: DaoFunctionSpec,
        returnType: DataTypeSpec?,
        isNested: Boolean
    ) {
        val actionSpec = functionSpec.actionSpec
        if (actionSpec == null) {
            addReturnSuperCall(
                functionSpec,
                returnType,
                isNested
            )

            return
        }

        val returnTypeSpec = returnType?.getNestedDataType()
        when (val dataType = returnTypeSpec?.dataType as? DataTypeSpec.DataType.Data) {
            is DataTypeSpec.DataType.Compound -> {
                val entitySpec = dataType.compoundSpec.mainProperty.dataTypeSpec
                    .getEntityDataType()
                    .entitySpec

                val query = when (actionSpec) {
                    is DaoActionSpec.QueryAction -> actionSpec.getSQLQuery(functionSpec)
                    is DaoActionSpec.EntityAction -> actionSpec.getSQLQuery(entitySpec)
                }

                addReturnCompoundLogic(
                    daoBuilder,
                    addedFunctions,
                    query,
                    returnType,
                    returnTypeSpec,
                    dataType.compoundSpec,
                    isNested
                )
            }

            is DataTypeSpec.DataType.Entity -> {
                val entitySpec = dataType.entitySpec
                val query = when (actionSpec) {
                    is DaoActionSpec.QueryAction -> actionSpec.getSQLQuery(functionSpec)
                    is DaoActionSpec.EntityAction -> actionSpec.getSQLQuery(dataType.entitySpec)
                }

                val functionName = entitySpec.getQueryFunctionName(query)
                val parameters = query.getParameterReferences()
                addEntityMapping(
                    parameters,
                    functionName,
                    returnType,
                    fullEntityName = null,
                    isNested
                )
            }

            is DataTypeSpec.DataType.Class,
            null -> {
                addReturnSimpleLogic(
                    functionSpec,
                    returnType,
                    isNested
                )
            }
        }
    }

    private fun CodeBlock.Builder.addReturnSuperCall(
        functionSpec: DaoFunctionSpec,
        returnType: DataTypeSpec?,
        isNested: Boolean
    ) {
        val functionName = functionSpec.declaration.simpleNameString
        val parameters = functionSpec.parameters.joinToString { parameter ->
            parameter.name
        }

        val superFunctionCall = "super.$functionName($parameters)"
        val returnFunctionCall =  if (returnType != null && !isNested) {
            "return·$superFunctionCall"
        } else {
            superFunctionCall
        }

        addStatement(returnFunctionCall)
    }

    private fun CodeBlock.Builder.addReturnSimpleLogic(
        functionSpec: DaoFunctionSpec,
        returnType: DataTypeSpec?,
        isNested: Boolean
    ) {
        val functionName = functionSpec.declaration.simpleNameString
        val parameters = functionSpec.parameters.joinToString { parameter ->
            parameter.name
        }

        val awaitFunction = returnType?.getAwaitFunction()
        val queryCall = if (awaitFunction.isNullOrBlank()) {
            "$daoQueriesPropertyName.$functionName($parameters)"
        } else {
            "$daoQueriesPropertyName.$functionName($parameters).$awaitFunction()"
        }

        val actualFunctionCall = if (returnType != null && !isNested) {
            "return·$queryCall"
        } else {
            queryCall
        }

        addStatement(actualFunctionCall)
    }

    private fun CodeBlock.Builder.addReturnCompoundLogic(
        daoBuilder: TypeSpec.Builder,
        addedFunctions: MutableSet<FunctionReference>,
        query: SQLQuery,
        returnType: DataTypeSpec,
        compoundReturnType: DataTypeSpec,
        compoundSpec: CompoundSpec,
        isNested: Boolean,
        isEntityQuerySkipped: Boolean = false
    ) {
        when (val type = returnType.dataType) {
            is DataTypeSpec.DataType.Wrapper -> {
                addCompoundPropertyMapping(
                    query,
                    compoundSpec.mainProperty,
                    compoundReturnType,
                    returnType.getAwaitFunction(),
                    isNested,
                    isEntityQuerySkipped
                )

                addReturnCompoundLogic(
                    daoBuilder,
                    addedFunctions,
                    query,
                    type.nestedTypeSpec,
                    compoundReturnType,
                    compoundSpec,
                    isNested = true,
                    isEntityQuerySkipped = true
                )

                if (compoundReturnType.isNullable) {
                    endControlFlow()
                }

                endControlFlow()
            }

            is DataTypeSpec.DataType.Compound -> {
                addCompoundMapping(
                    daoBuilder,
                    addedFunctions,
                    query,
                    compoundSpec,
                    isNested,
                    isEntityQuerySkipped,
                    isForReturn = true
                )
            }

            is DataTypeSpec.DataType.Entity,
            is DataTypeSpec.DataType.Class -> error("not supported here")
        }
    }

    private fun CodeBlock.Builder.addCompoundMapping(
        daoBuilder: TypeSpec.Builder,
        addedFunctions: MutableSet<FunctionReference>,
        query: SQLQuery,
        compoundSpec: CompoundSpec,
        isNested: Boolean,
        isEntityQuerySkipped: Boolean,
        isForReturn: Boolean,
        mainEntitySpec: MainEntitySpec? = null,
        relationSpec: CompoundRelationSpec? = null,
        parents: Set<CompoundPropertySpec> = LinkedHashSet(),
    ): MainEntitySpec {
        val newMainEntitySpec = addMainPropertyMapping(
            daoBuilder,
            addedFunctions,
            query,
            compoundSpec,
            isNested,
            isEntityQuerySkipped,
            mainEntitySpec,
            relationSpec,
            parents
        )

        compoundSpec.relations.forEach { compoundRelationSpec ->
            val dataTypeSpec = compoundRelationSpec.property.dataTypeSpec.getNestedDataType()
            val property = compoundRelationSpec.property
            val newParents = parents + property
            val fullEntityName = newParents.asName()
            val mainPropertyName = newMainEntitySpec.parents.asName()
            val parentColumnAccess = newMainEntitySpec.spec
                .getColumnAccessChain(compoundRelationSpec.relation.parentColumn)
            val parametersAccess = parentColumnAccess.toParameterAccess()

            val getter = when (val type = dataTypeSpec.dataType as DataTypeSpec.DataType.Data) {
                is DataTypeSpec.DataType.Class -> error("not supported here")
                is DataTypeSpec.DataType.Compound -> {
                    val functionReference = daoBuilder.addCompoundRelationFunction(
                        addedFunctions,
                        compoundRelationSpec,
                        type.compoundSpec,
                        dataTypeSpec
                    )

                    getFunctionCall(
                        functionReference.name,
                        parametersAccess,
                        mainPropertyName,
                        parentColumnAccess.isNullableAccess,
                        isArgumentNullable = false,
                        functionOwner = null,
                        chainFunctionCall = null
                    )
                }

                is DataTypeSpec.DataType.Entity -> {
                    val entitySpec = type.entitySpec
                    val entityColumnAccess = entitySpec
                        .getColumnAccessChain(compoundRelationSpec.relation.entityColumn)

                    val functionName = entitySpec.getQueryByColumnsName(entityColumnAccess.last())
                    val awaitFunction = property.dataTypeSpec.getAwaitFunction()
                    getFunctionCall(
                        functionName,
                        parametersAccess,
                        mainPropertyName,
                        parentColumnAccess.isNullableAccess,
                        entityColumnAccess.isNullableAccess,
                        daoQueriesPropertyName,
                        awaitFunction
                    )
                }
            }

            add("val·$fullEntityName·=·")
            add(getter)
        }

        val initParameters = ArrayList<String>()
        initParameters.add((parents + compoundSpec.mainProperty).asName())
        compoundSpec.relations.forEach { relation ->
            val name = (parents + relation.property).asName()
            initParameters.add(name)
        }

        val initialization = typeInitializer(initParameters)
        val statement = when {
            !isForReturn -> {
                val name = parents.asName()
                "val·$name·=·$initialization"
            }

            isNested -> initialization
            else -> "return·$initialization"
        }

        addStatement(statement, compoundSpec.declaration.toClassName())
        return newMainEntitySpec
    }

    private fun getFunctionCall(
        name: String,
        parametersAccess: String,
        parametersOwner: String?,
        isParameterNullable: Boolean,
        isArgumentNullable: Boolean,
        functionOwner: String?,
        chainFunctionCall: String?
    ): CodeBlock {
        val handleNullability = !isArgumentNullable && isParameterNullable

        val fullParameterAccess = buildString {
            if (!parametersOwner.isNullOrBlank()) {
                append(parametersOwner, SYMBOL_ACCESS_SIGN)
            }

            append(parametersAccess)
        }

        val fullFunctionAccess = buildString {
            if (!functionOwner.isNullOrBlank()) {
                append(functionOwner, SYMBOL_ACCESS_SIGN)
            }

            append(name)
        }

        return CodeBlock.builder().apply {
            if (handleNullability) {
                val lastParameterName = fullParameterAccess.substringAfterLast(SYMBOL_ACCESS_SIGN)
                beginControlFlow("$fullParameterAccess?.let·{·$lastParameterName·->")
                val functionCall = "$fullFunctionAccess($lastParameterName)"
                val finalFunctionCall = if (!chainFunctionCall.isNullOrBlank()) {
                    "$functionCall.$chainFunctionCall()"
                } else {
                    functionCall
                }

                addStatement(finalFunctionCall)
                endControlFlow()
            } else {
                val functionCall = "$fullFunctionAccess($fullParameterAccess)"
                val finalFunctionCall = if (!chainFunctionCall.isNullOrBlank()) {
                    "$functionCall.$chainFunctionCall()"
                } else {
                    functionCall
                }

                addStatement(finalFunctionCall)
            }
        }.build()
    }

    private fun TypeSpec.Builder.addCompoundRelationFunction(
        addedFunctions: MutableSet<FunctionReference>,
        relationSpec: CompoundRelationSpec,
        compoundSpec: CompoundSpec,
        compoundReturnType: DataTypeSpec
    ): FunctionReference {
        val returnType = relationSpec.property.dataTypeSpec.type.toTypeName()
            .copy(nullable = false)

        val entitySpec = compoundSpec
            .mainProperty
            .dataTypeSpec
            .getEntityDataType()
            .entitySpec

        val entityColumnAccess = entitySpec
            .getColumnAccessChain(relationSpec.relation.entityColumn)

        val functionName = compoundSpec.declaration
            .getQueryByColumnsName(entityColumnAccess.toSortedSet())

        val actualFunctionName = if (relationSpec.property.dataTypeSpec.dataType is DataTypeSpec.DataType.Collection) {
            buildString {
                append(functionName, "Collection")
            }
        } else {
            functionName
        }

        val directColumn = entityColumnAccess.last()
        val parameterReference = ParameterReference(
            directColumn.declaration.simpleNameString,
            directColumn.declaration.type.toTypeName()
        )

        val reference = FunctionReference(actualFunctionName, listOf(parameterReference), returnType)
        if (addedFunctions.contains(reference)) {
            return reference
        }

        addedFunctions.add(reference)
        val functionBuilder = FunSpec.builder(actualFunctionName)
            .addModifiers(KModifier.SUSPEND, KModifier.PRIVATE)
            .returns(returnType)
            .addParameter(parameterReference.name, parameterReference.type)

        val functionCodeBuilder = CodeBlock.builder()
        val query = getSelectSQLQuery(entitySpec, directColumn)
        functionCodeBuilder.addReturnCompoundLogic(
            this,
            addedFunctions,
            query,
            relationSpec.property.dataTypeSpec,
            compoundReturnType,
            compoundSpec,
            isNested = false,
            isEntityQuerySkipped = false
        )

        functionBuilder.addCode(functionCodeBuilder.build())
        addFunction(functionBuilder.build())

        return reference
    }

    data class MainEntitySpec(
        val spec: EntitySpec,
        val property: CompoundPropertySpec,
        val parents: Set<CompoundPropertySpec>
    )

    private fun CodeBlock.Builder.addMainPropertyMapping(
        daoBuilder: TypeSpec.Builder,
        addedFunctions: MutableSet<FunctionReference>,
        query: SQLQuery,
        compoundSpec: CompoundSpec,
        isNested: Boolean,
        isEntityQuerySkipped: Boolean,
        mainEntitySpec: MainEntitySpec?,
        relationSpec: CompoundRelationSpec?,
        parents: Set<CompoundPropertySpec>
    ): MainEntitySpec {
        val property = compoundSpec.mainProperty
        val newParents = parents + property
        val fullEntityName = newParents.asName()

        return when (val mainPropertyType = property.dataTypeSpec.dataType) {
            is DataTypeSpec.DataType.Compound -> {
                return addCompoundMapping(
                    daoBuilder,
                    addedFunctions,
                    query,
                    mainPropertyType.compoundSpec,
                    isNested,
                    isEntityQuerySkipped,
                    isForReturn = false,
                    mainEntitySpec,
                    relationSpec,
                    newParents
                )
            }

            is DataTypeSpec.DataType.Entity -> {
                if (!isEntityQuerySkipped) {
                    val functionName = mainPropertyType.entitySpec.getQueryFunctionName(query)
                  /*  if (functionName == "queryDocumentEntityByIdentityIdIdentityTypeIdentityOptionalMimeTypeIdentityOptionalFallBackMimeTypeIdentityOptionalVolumeIdIdentityOptionalAccessHashIdentityOptionalDatacenterId") {
                        logger.throwException("Error with function name: $functionName from query $query")
                    }*/
                    addEntityMapping(
                        query.getParameterReferences(),
                        functionName,
                        property.dataTypeSpec,
                        fullEntityName,
                        isNested
                    )
                }

                MainEntitySpec(
                    mainPropertyType.entitySpec,
                    property,
                    newParents
                )
            }

            is DataTypeSpec.DataType.Class,
            is DataTypeSpec.DataType.Collection,
            is DataTypeSpec.DataType.Stream -> {
                logger.throwException(
                    "This type is not supported as a main entity for compounds: ${compoundSpec.mainProperty.dataTypeSpec}",
                    compoundSpec.mainProperty.declaration
                )
            }
        }
    }

    private fun CodeBlock.Builder.addEntityMapping(
        parameters: List<ParameterReference>,
        functionName: String,
        returnDataTypeSpec: DataTypeSpec,
        fullEntityName: String?,
        isNested: Boolean
    ) {
        val parametersCall = parameters.getParametersCall()
        val awaitFunction = returnDataTypeSpec.getAwaitFunction()
        val functionCall = "$daoQueriesPropertyName.$functionName($parametersCall).$awaitFunction()"
        val statement = when {
            fullEntityName.isNullOrBlank() -> if (isNested) {
                functionCall
            } else {
                "return·$functionCall"
            }

            else -> {
                "val·$fullEntityName·=·$functionCall"
            }
        }

        addStatement(statement)
    }

    private fun CodeBlock.Builder.addCompoundPropertyMapping(
        query: SQLQuery,
        property: CompoundPropertySpec,
        dataReturnType: DataTypeSpec,
        awaitFunction: String?,
        isNested: Boolean,
        isEntityQuerySkipped: Boolean
    ) {
        val newParents = LinkedHashSet<CompoundPropertySpec>()
        newParents.add(property)

        var currentProperty = property
        while (currentProperty.dataTypeSpec.dataType !is DataTypeSpec.DataType.Entity) {
            val compoundType = currentProperty.dataTypeSpec.dataType as DataTypeSpec.DataType.Compound
            currentProperty = compoundType.compoundSpec.mainProperty
            newParents.add(currentProperty)
        }

        val entityDataType = currentProperty.dataTypeSpec.dataType as DataTypeSpec.DataType.Entity
        val entitySpec = entityDataType.entitySpec
        val propertyName = newParents.asName()
        val functionCall = if (isEntityQuerySkipped) {
            propertyName
        } else {
            val functionName = entitySpec.getQueryFunctionName(query)
            val parametersCall = query.getParameterReferences().getParametersCall()
            val actualAwaitFunction = awaitFunction ?: property.dataTypeSpec.getAwaitFunction()
            val functionCall = "$daoQueriesPropertyName.$functionName($parametersCall).$actualAwaitFunction()"
            if (isNested) {
                functionCall
            } else {
                "return·$functionCall"
            }
        }

        beginControlFlow("$functionCall.map·{·$propertyName·->")

        if (dataReturnType.isNullable) {
            beginControlFlow("$propertyName?.let·{")
        }
    }

    private fun DataTypeSpec.getAwaitFunction(): String = when (val type = dataType) {
        is DataTypeSpec.DataType.Data -> if (isNullable) {
            "awaitAsOneOrNullIO"
        } else {
            "awaitAsOneNotNullIO"
        }

        is DataTypeSpec.DataType.Collection -> "awaitAsListIO"
        is DataTypeSpec.DataType.Stream -> when (type.nestedTypeSpec.dataType) {
            is DataTypeSpec.DataType.Collection -> "asFlowIOList"
            is DataTypeSpec.DataType.Data -> if (type.nestedTypeSpec.isNullable) {
                "asFlowIONullable"
            } else {
                "asFlowIONotNull"
            }

            is DataTypeSpec.DataType.Stream -> {
                logger.throwException("Nested streams are not supported",
                    this.reference
                )
            }
        }
    }

    data class Result(
        val className: ClassName
    )
}
