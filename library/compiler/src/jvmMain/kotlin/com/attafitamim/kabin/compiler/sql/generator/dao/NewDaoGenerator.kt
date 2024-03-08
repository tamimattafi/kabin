package com.attafitamim.kabin.compiler.sql.generator.dao

import com.attafitamim.kabin.compiler.sql.generator.references.FunctionReference
import com.attafitamim.kabin.compiler.sql.generator.references.ParameterReference
import com.attafitamim.kabin.compiler.sql.utils.poet.buildSpec
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.asName
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.getColumnParameterAccess
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.getCompoundFunctionName
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.getParametersCall
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.toReferences
import com.attafitamim.kabin.compiler.sql.utils.poet.simpleNameString
import com.attafitamim.kabin.compiler.sql.utils.poet.toPascalCase
import com.attafitamim.kabin.compiler.sql.utils.poet.typeInitializer
import com.attafitamim.kabin.compiler.sql.utils.poet.writeType
import com.attafitamim.kabin.compiler.sql.utils.spec.getDataReturnType
import com.attafitamim.kabin.core.dao.KabinDao
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.utils.throwException
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

class NewDaoGenerator(
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
            if (functionSpec.actionSpec == null && !isTransaction) {
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

        val returnTypeSpec = returnType?.getDataReturnType()
        val returnTypeDataType = returnTypeSpec?.dataType
        if (returnTypeDataType is DataTypeSpec.DataType.Compound) {
            addReturnCompoundLogic(
                daoBuilder,
                addedFunctions,
                functionSpec.parameters.toReferences(),
                returnType,
                returnTypeSpec,
                returnTypeDataType.spec,
                isNested
            )
        } else {
            addReturnSimpleLogic(
                functionSpec,
                returnType,
                isNested
            )
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
        parameters: List<ParameterReference>,
        returnType: DataTypeSpec,
        compoundReturnType: DataTypeSpec,
        compoundSpec: CompoundSpec,
        isNested: Boolean,
        isEntityQuerySkipped: Boolean = false
    ) {
        when (val type = returnType.dataType) {
            is DataTypeSpec.DataType.Wrapper -> {
                addCompoundPropertyMapping(
                    parameters,
                    compoundSpec.mainProperty,
                    compoundReturnType,
                    returnType.getAwaitFunction(),
                    isNested,
                    isEntityQuerySkipped
                )

                addReturnCompoundLogic(
                    daoBuilder,
                    addedFunctions,
                    parameters,
                    type.wrappedDeclaration,
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
                    parameters,
                    compoundSpec,
                    isNested,
                    isEntityQuerySkipped
                )
            }

            is DataTypeSpec.DataType.Entity,
            is DataTypeSpec.DataType.Class -> error("not supported here")
        }
    }

    private fun CodeBlock.Builder.addCompoundMapping(
        daoBuilder: TypeSpec.Builder,
        addedFunctions: MutableSet<FunctionReference>,
        parameters: List<ParameterReference>,
        compoundSpec: CompoundSpec,
        isNested: Boolean,
        isEntityQuerySkipped: Boolean,
        mainEntitySpec: MainEntitySpec? = null,
        relationSpec: CompoundRelationSpec? = null,
        parents: Set<CompoundPropertySpec> = LinkedHashSet(),
    ): MainEntitySpec {
        val newMainEntitySpec = addMainPropertyMapping(
            daoBuilder,
            addedFunctions,
            parameters,
            compoundSpec,
            isNested,
            isEntityQuerySkipped,
            mainEntitySpec,
            relationSpec,
            parents
        )

        compoundSpec.relations.forEach { relationSpec ->
            val dataTypeSpec = relationSpec.property.dataTypeSpec.getDataReturnType()
            val property = relationSpec.property
            val newParents = parents + property
            val fullEntityName = newParents.asName()

            val getter = when (val type = dataTypeSpec.dataType as DataTypeSpec.DataType.Data) {
                is DataTypeSpec.DataType.Class -> error("not supported here")
                is DataTypeSpec.DataType.Compound -> {
                    val functionReference = daoBuilder.addCompoundRelationFunction(
                        addedFunctions,
                        relationSpec,
                        type.spec,
                        dataTypeSpec
                    )

                    "${functionReference.name}(${relationSpec.relation.parentColumn})"
                }

                is DataTypeSpec.DataType.Entity -> {
                    val mainPropertyName = newMainEntitySpec.parents.asName()
                    val columnAccess = newMainEntitySpec.spec
                        .getColumnParameterAccess(relationSpec.relation.parentColumn)

                    val functionName = buildString {
                        append("get")
                        append(type.spec.declaration.simpleNameString)

                        append("By")

                        parameters.forEach { parameterReference ->
                            append(parameterReference.name.toPascalCase())
                        }
                    }

                    val awaitFunction = property.dataTypeSpec.getAwaitFunction()
                    "$daoQueriesPropertyName.$functionName($mainPropertyName.$columnAccess).$awaitFunction()"
                }
            }

            addStatement("val $fullEntityName = $getter")
        }


        val initParameters = ArrayList<String>()
        initParameters.add((parents + compoundSpec.mainProperty).asName())
        compoundSpec.relations.forEach { relation ->
            val name = (parents + relation.property).asName()
            initParameters.add(name)
        }

        val initialization = typeInitializer(initParameters)
        val statement = if (isNested) {
            initialization
        } else {
            "return·$initialization"
        }

        addStatement(statement, compoundSpec.declaration.toClassName())
        return newMainEntitySpec
    }

    private fun TypeSpec.Builder.addCompoundRelationFunction(
        addedFunctions: MutableSet<FunctionReference>,
        relationSpec: CompoundRelationSpec,
        compoundSpec: CompoundSpec,
        compoundReturnType: DataTypeSpec
    ): FunctionReference {
        val returnType = relationSpec.property.dataTypeSpec.type.toTypeName()
            .copy(nullable = false)

        val entityColumnAccess = compoundSpec
            .mainProperty
            .dataTypeSpec
            .getColumnParameterAccess(relationSpec.relation.entityColumn)

        val functionName = buildString {
            append("get")
            append(compoundSpec.declaration.simpleNameString)

            if (relationSpec.property.dataTypeSpec.dataType is DataTypeSpec.DataType.Collection) {
                append("Collection")
            }

            append("By")

            entityColumnAccess.forEach { columnSpec ->
                append(columnSpec.declaration.simpleNameString.toPascalCase())
            }
        }

        val directColumn = entityColumnAccess.last()
        val parameterReference = ParameterReference(
            directColumn.declaration.simpleNameString,
            directColumn.declaration.type.toTypeName()
        )

        val reference = FunctionReference(functionName, listOf(parameterReference), returnType)
        if (addedFunctions.contains(reference)) {
            return reference
        }

        addedFunctions.add(reference)
        val functionBuilder = FunSpec.builder(functionName)
            .addModifiers(KModifier.SUSPEND, KModifier.PRIVATE)
            .returns(returnType)
            .addParameter(parameterReference.name, parameterReference.type)

        val functionCodeBuilder = CodeBlock.builder()
        functionCodeBuilder.addReturnCompoundLogic(
            this,
            addedFunctions,
            reference.parameters,
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
        parameters: List<ParameterReference>,
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
                    parameters,
                    mainPropertyType.spec,
                    isNested,
                    isEntityQuerySkipped,
                    mainEntitySpec,
                    relationSpec,
                    newParents
                )
            }

            is DataTypeSpec.DataType.Entity -> {
                if (!isEntityQuerySkipped) {
                    val parametersCall = parameters.getParametersCall()
                    val functionName = buildString {
                        append("get")
                        append(mainPropertyType.spec.declaration.simpleNameString)

                        append("By")

                        parameters.forEach { parameterReference ->
                            append(parameterReference.name.toPascalCase())
                        }
                    }

                    val awaitFunction = property.dataTypeSpec.getAwaitFunction()
                    addStatement("val·$fullEntityName·=·$daoQueriesPropertyName.$functionName($parametersCall).$awaitFunction()")
                }

                MainEntitySpec(
                    mainPropertyType.spec,
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

    private fun CodeBlock.Builder.addCompoundNullableMapping(
        functionSpec: DaoFunctionSpec,
        parentProperty: CompoundPropertySpec,
        property: CompoundPropertySpec,
        mainEntitySpec: MainEntitySpec,
        relationSpec: CompoundRelationSpec,
        parents: Set<CompoundPropertySpec>
    ) {
        val newParents = LinkedHashSet<CompoundPropertySpec>(parents)
        newParents.add(property)

        var currentProperty = property
        while (currentProperty.dataTypeSpec.dataType !is DataTypeSpec.DataType.Entity) {
            val compoundType = property.dataTypeSpec.dataType as DataTypeSpec.DataType.Compound
            currentProperty = compoundType.spec.mainProperty
            newParents.add(currentProperty)
        }

        val propertyName = newParents.asName()
        val functionName = functionSpec.getCompoundFunctionName(newParents)
        val awaitFunction = parentProperty.dataTypeSpec.getAwaitFunction()

        val parentName = parents.asName()
        val mainPropertyName = mainEntitySpec.parents.asName()
        val columnAccess = mainEntitySpec.spec
            .getColumnParameterAccess(relationSpec.relation.parentColumn)

        val functionCall = "$daoQueriesPropertyName.$functionName($mainPropertyName.$columnAccess).$awaitFunction()"
        addStatement("val·$propertyName·=·$functionCall")
        beginControlFlow("val·$parentName·=·$propertyName?.let·{·$propertyName·->")
    }

    private fun CodeBlock.Builder.addCompoundPropertyMapping(
        parameters: List<ParameterReference>,
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
            val compoundType = property.dataTypeSpec.dataType as DataTypeSpec.DataType.Compound
            currentProperty = compoundType.spec.mainProperty
            newParents.add(currentProperty)
        }

        val entityDataType = currentProperty.dataTypeSpec.dataType as DataTypeSpec.DataType.Entity
        val propertyName = newParents.asName()
        val functionCall = if (isEntityQuerySkipped) {
            propertyName
        } else {
            val functionName = buildString {
                append("get")
                append(entityDataType.spec.declaration.simpleNameString)

                append("By")

                parameters.forEach { parameterReference ->
                    append(parameterReference.name.toPascalCase())
                }
            }

            val parametersCall = parameters.getParametersCall()
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
        is DataTypeSpec.DataType.Stream -> when (type.wrappedDeclaration.dataType) {
            is DataTypeSpec.DataType.Collection -> "asFlowIOList"
            is DataTypeSpec.DataType.Data -> if (type.wrappedDeclaration.isNullable) {
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
