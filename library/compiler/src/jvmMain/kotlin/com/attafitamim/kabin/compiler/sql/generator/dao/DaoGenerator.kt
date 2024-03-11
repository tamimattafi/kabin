package com.attafitamim.kabin.compiler.sql.generator.dao

import app.cash.sqldelight.ColumnAdapter
import com.attafitamim.kabin.compiler.sql.generator.references.ColumnAdapterReference
import com.attafitamim.kabin.compiler.sql.generator.references.FunctionReference
import com.attafitamim.kabin.compiler.sql.generator.references.ParameterReference
import com.attafitamim.kabin.compiler.sql.syntax.SQLQuery
import com.attafitamim.kabin.compiler.sql.utils.poet.SYMBOL_ACCESS_SIGN
import com.attafitamim.kabin.compiler.sql.utils.poet.buildSpec
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.asName
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.getAdapterReference
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.getColumnAccessChain
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.getParametersCall
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.isNullableAccess
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.toParameterAccess
import com.attafitamim.kabin.compiler.sql.utils.poet.references.getPropertyName
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
import com.attafitamim.kabin.specs.column.ColumnSpec
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

        val addedFunctions = HashSet<FunctionReference>()
        val adapters = LinkedHashSet<ColumnAdapterReference>()
        daoSpec.functionSpecs.forEach { functionSpec ->
            val isTransaction = functionSpec.isTransactionRequired()
            val actionSpec = functionSpec.actionSpec
            if (actionSpec == null && !isTransaction) {
                return@forEach
            }

            val functionCodeBuilder = CodeBlock.builder()
            val returnType = functionSpec.returnTypeSpec
            if (isTransaction) {
                if (returnType != null) {
                    functionCodeBuilder.beginControlFlow("return·transactionWithResult·{")
                } else {
                    functionCodeBuilder.beginControlFlow("transaction·{")
                }
            }

            functionCodeBuilder.addReturnLogic(
                classBuilder,
                addedFunctions,
                adapters,
                functionSpec,
                returnType,
                isTransaction
            )

            if (isTransaction) {
                functionCodeBuilder.endControlFlow()
            }

            val functionBuilder = functionSpec.declaration.buildSpec()
                .addModifiers(KModifier.OVERRIDE)
                .addCode(functionCodeBuilder.build())
                .build()

            classBuilder.addFunction(functionBuilder)
        }

        val constructorBuilder = FunSpec.constructorBuilder()
            .addParameter(daoQueriesPropertyName, daoQueriesClassName)

        val daoQueriesPropertySpec = PropertySpec.builder(
            daoQueriesPropertyName,
            daoQueriesClassName,
            KModifier.OVERRIDE
        ).initializer(daoQueriesPropertyName).build()

        adapters.forEach { adapter ->
            val propertyName = adapter.getPropertyName()
            val adapterType = ColumnAdapter::class.asClassName()
                .parameterizedBy(adapter.kotlinType, adapter.affinityType)

            val propertySpec = PropertySpec.builder(
                propertyName,
                adapterType,
                KModifier.PRIVATE
            ).initializer(propertyName).build()

            classBuilder.addProperty(propertySpec)

            constructorBuilder.addParameter(
                propertyName,
                adapterType
            )
        }

        classBuilder
            .primaryConstructor(constructorBuilder.build())
            .addProperty(daoQueriesPropertySpec)

        codeGenerator.writeType(
            className,
            classBuilder.build()
        )

        return Result(className, adapters)
    }

    private fun DaoFunctionSpec.isTransactionRequired(): Boolean {
        // Temporary, until problem with transactions is fixed
        return false
        if (transactionSpec != null) {
            return true
        }

        val actionSpec = actionSpec
        if (actionSpec is DaoActionSpec.EntityAction) {
            return parameters.size > 1 || parameters.any { parameterSpec ->
                when (parameterSpec.typeSpec.dataType) {
                    is DataTypeSpec.DataType.Collection,
                    is DataTypeSpec.DataType.Compound -> true
                    is DataTypeSpec.DataType.Entity -> false
                    is DataTypeSpec.DataType.Class,
                    is DataTypeSpec.DataType.Stream -> logger.throwException(
                        "Only entity types are supported as parameters for this annotation",
                        declaration
                    )
                }
            }
        }

        return when (returnTypeSpec?.dataType) {
            is DataTypeSpec.DataType.Compound,
            is DataTypeSpec.DataType.Collection -> true
            is DataTypeSpec.DataType.Stream,
            is DataTypeSpec.DataType.Entity,
            is DataTypeSpec.DataType.Class,
            null -> false
        }
    }

    private fun CodeBlock.Builder.addReturnLogic(
        daoBuilder: TypeSpec.Builder,
        addedFunctions: MutableSet<FunctionReference>,
        adapters: MutableSet<ColumnAdapterReference>,
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
                    is DaoActionSpec.QueryAction -> actionSpec.getSQLQuery(functionSpec, logger)
                    is DaoActionSpec.EntityAction -> actionSpec.getSQLQuery(entitySpec)
                }

                addReturnCompoundLogic(
                    daoBuilder,
                    addedFunctions,
                    adapters,
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
                    is DaoActionSpec.QueryAction -> actionSpec.getSQLQuery(functionSpec, logger)
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
        adapters: MutableSet<ColumnAdapterReference>,
        query: SQLQuery,
        returnType: DataTypeSpec,
        compoundReturnType: DataTypeSpec,
        compoundSpec: CompoundSpec,
        isNested: Boolean,
        isEntityQuerySkipped: Boolean = false
    ) {
        when (val type = returnType.dataType) {
            is DataTypeSpec.DataType.Wrapper -> {
                addCompoundWrapperMapping(
                    query,
                    compoundSpec.mainProperty,
                    returnType.getAwaitFunction(),
                    isNested,
                    isEntityQuerySkipped
                )

                addReturnCompoundLogic(
                    daoBuilder,
                    addedFunctions,
                    adapters,
                    query,
                    type.nestedTypeSpec,
                    compoundReturnType,
                    compoundSpec,
                    isNested = true,
                    isEntityQuerySkipped = true
                )

                endControlFlow()
            }

            is DataTypeSpec.DataType.Compound -> {
                if (compoundReturnType.isNullable) {
                    addCompoundNullableMapping(
                        query,
                        compoundSpec.mainProperty,
                        returnType.getAwaitFunction(),
                        isNested,
                        isEntityQuerySkipped
                    )
                }

                val skipEntityQuery = isEntityQuerySkipped ||
                        compoundReturnType.isNullable

                addCompoundMapping(
                    daoBuilder,
                    addedFunctions,
                    adapters,
                    query,
                    compoundSpec,
                    isNested,
                    skipEntityQuery,
                    isForReturn = true
                )

                if (compoundReturnType.isNullable) {
                    endControlFlow()
                }
            }

            is DataTypeSpec.DataType.Entity,
            is DataTypeSpec.DataType.Class -> error("not supported here")
        }
    }

    private fun CodeBlock.Builder.addCompoundMapping(
        daoBuilder: TypeSpec.Builder,
        addedFunctions: MutableSet<FunctionReference>,
        adapters: MutableSet<ColumnAdapterReference>,
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
            adapters,
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
                    val entitySpec = type
                        .compoundSpec
                        .mainProperty
                        .dataTypeSpec
                        .getEntityDataType()
                        .entitySpec

                    val entityColumnAccess = entitySpec
                        .getColumnAccessChain(compoundRelationSpec.relation.entityColumn)

                    val functionReference = daoBuilder.addCompoundRelationFunction(
                        addedFunctions,
                        adapters,
                        compoundRelationSpec,
                        type.compoundSpec,
                        entitySpec,
                        entityColumnAccess,
                        dataTypeSpec
                    )

                    val directParentColumn = parentColumnAccess.last()
                    val directEntityColumn = entityColumnAccess.last()
                    val adapter = directParentColumn
                        .getAdapterReference(directEntityColumn)

                    adapter?.let(adapters::add)
                    getFunctionCall(
                        functionReference.name,
                        parametersAccess,
                        mainPropertyName,
                        parentColumnAccess.isNullableAccess,
                        isArgumentNullable = false,
                        functionOwner = null,
                        chainFunctionCall = null,
                        adapter
                    )
                }

                is DataTypeSpec.DataType.Entity -> {
                    val entitySpec = type.entitySpec
                    val entityColumnAccess = entitySpec
                        .getColumnAccessChain(compoundRelationSpec.relation.entityColumn)

                    val functionName = entitySpec.getQueryByColumnsName(entityColumnAccess.last())
                    val awaitFunction = property.dataTypeSpec.getAwaitFunction()

                    val directParentColumn = parentColumnAccess.last()
                    val directEntityColumn = entityColumnAccess.last()
                    val adapter = directParentColumn
                        .getAdapterReference(directEntityColumn)

                    adapter?.let(adapters::add)
                    getFunctionCall(
                        functionName,
                        parametersAccess,
                        mainPropertyName,
                        parentColumnAccess.isNullableAccess,
                        entityColumnAccess.isNullableAccess,
                        daoQueriesPropertyName,
                        awaitFunction,
                        adapter
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
        chainFunctionCall: String?,
        adapterReference: ColumnAdapterReference?
    ): CodeBlock {
        val adapterProperty = adapterReference?.getPropertyName()
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
                val fullAccessWithEncode = if (adapterProperty == null) {
                    lastParameterName
                } else buildString {
                    append("$adapterProperty.encode($lastParameterName)")
                }
                val functionCall = "$fullFunctionAccess($fullAccessWithEncode)"
                val finalFunctionCall = if (!chainFunctionCall.isNullOrBlank()) {
                    "$functionCall.$chainFunctionCall()"
                } else {
                    functionCall
                }

                addStatement(finalFunctionCall)
                endControlFlow()
            } else {
                val fullAccessWithEncode = if (adapterProperty == null) {
                    fullParameterAccess
                } else buildString {
                    append("$adapterProperty.encode($fullParameterAccess)")
                }

                val functionCall = "$fullFunctionAccess($fullAccessWithEncode)"
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
        adapters: MutableSet<ColumnAdapterReference>,
        relationSpec: CompoundRelationSpec,
        compoundSpec: CompoundSpec,
        entitySpec: EntitySpec,
        entityColumnAccess: List<ColumnSpec>,
        compoundReturnType: DataTypeSpec
    ): FunctionReference {
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

        val reference = FunctionReference(actualFunctionName, listOf(parameterReference))
        if (addedFunctions.contains(reference)) {
            return reference
        }

        addedFunctions.add(reference)

        val returnType = relationSpec.property.dataTypeSpec.type.toTypeName()
        val functionBuilder = FunSpec.builder(actualFunctionName)
            .addModifiers(KModifier.SUSPEND, KModifier.PRIVATE)
            .returns(returnType)
            .addParameter(parameterReference.name, parameterReference.type)

        val functionCodeBuilder = CodeBlock.builder()
        val query = getSelectSQLQuery(entitySpec, directColumn)
        functionCodeBuilder.addReturnCompoundLogic(
            this,
            addedFunctions,
            adapters,
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
        adapters: MutableSet<ColumnAdapterReference>,
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
                    adapters,
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

    private fun CodeBlock.Builder.addCompoundWrapperMapping(
        query: SQLQuery,
        property: CompoundPropertySpec,
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
    }


    private fun CodeBlock.Builder.addCompoundNullableMapping(
        query: SQLQuery,
        property: CompoundPropertySpec,
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
            "$propertyName?.let·{"
        } else {
            val functionName = entitySpec.getQueryFunctionName(query)
            val parametersCall = query.getParameterReferences().getParametersCall()
            val actualAwaitFunction = awaitFunction ?: property.dataTypeSpec.getAwaitFunction()
            "$daoQueriesPropertyName.$functionName($parametersCall).$actualAwaitFunction()?.let·{·$propertyName·->"
        }

        val returnStatement = if (isNested) {
            functionCall
        } else {
            "return·$functionCall"
        }

        beginControlFlow(returnStatement)
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
        val className: ClassName,
        val adapters: Set<ColumnAdapterReference>
    )
}
