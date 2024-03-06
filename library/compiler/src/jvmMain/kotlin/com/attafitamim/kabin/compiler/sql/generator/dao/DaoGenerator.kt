package com.attafitamim.kabin.compiler.sql.generator.dao

import com.attafitamim.kabin.compiler.sql.utils.poet.buildSpec
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.asName
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.getColumnParameterAccess
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.getCompoundFunctionName
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.getParametersCall
import com.attafitamim.kabin.compiler.sql.utils.poet.simpleNameString
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

        daoSpec.functionSpecs.forEach { functionSpec ->
            val isTransaction = functionSpec.transactionSpec != null
            if (functionSpec.actionSpec == null && !isTransaction) {
                return@forEach
            }

            val functionCodeBuilder = CodeBlock.builder()
            val returnType = functionSpec.returnTypeSpec
            if (isTransaction) {
                if (returnType != null) {
                    functionCodeBuilder.beginControlFlow("return transactionWithResult")
                } else {
                    functionCodeBuilder.beginControlFlow("transaction")
                }
            }

            functionCodeBuilder.addReturnLogic(
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
                functionSpec,
                returnType,
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
            "return $superFunctionCall"
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
            "return $queryCall"
        } else {
            queryCall
        }

        addStatement(actualFunctionCall)
    }

    private fun CodeBlock.Builder.addReturnCompoundLogic(
        functionSpec: DaoFunctionSpec,
        returnType: DataTypeSpec,
        compoundSpec: CompoundSpec,
        isNested: Boolean,
        isEntityQuerySkipped: Boolean = false
    ) {
        when (val type = returnType.dataType) {
            is DataTypeSpec.DataType.Wrapper -> {
                addCompoundPropertyMapping(
                    functionSpec,
                    compoundSpec.mainProperty,
                    returnType.getAwaitFunction(),
                    isNested,
                    isEntityQuerySkipped
                )

                addReturnCompoundLogic(
                    functionSpec,
                    type.wrappedDeclaration,
                    compoundSpec,
                    isNested = true,
                    isEntityQuerySkipped = true
                )

                endControlFlow()
            }

            is DataTypeSpec.DataType.Compound -> {
                addCompoundMapping(
                    functionSpec,
                    compoundSpec,
                    isNested,
                    isForReturn = true,
                    isEntityQuerySkipped
                )
            }

            is DataTypeSpec.DataType.Entity,
            is DataTypeSpec.DataType.Class -> error("not supported here")
        }
    }

    private fun CodeBlock.Builder.addCompoundMapping(
        functionSpec: DaoFunctionSpec,
        compoundSpec: CompoundSpec,
        isNested: Boolean,
        isForReturn: Boolean,
        isEntityQuerySkipped: Boolean,
        mainEntitySpec: MainEntitySpec? = null,
        relationSpec: CompoundRelationSpec? = null,
        parents: Set<CompoundPropertySpec> = LinkedHashSet(),
    ): MainEntitySpec {
        val newMainEntitySpec = addMainPropertyMapping(
            functionSpec,
            compoundSpec,
            isNested,
            isEntityQuerySkipped,
            mainEntitySpec,
            relationSpec,
            parents
        )

        addRelationsMapping(
            functionSpec,
            compoundSpec,
            newMainEntitySpec,
            parents
        )

        val parameters = ArrayList<String>()
        parameters.add((parents + compoundSpec.mainProperty).asName())
        compoundSpec.relations.forEach { relation ->
            val name = (parents + relation.property).asName()
            parameters.add(name)
        }

        val initialization = typeInitializer(parameters)
        val statement = when {
            isForReturn -> if (isNested) {
                initialization
            } else {
                "return $initialization"
            }

            else -> {
                val fullEntityName = parents.asName()
                "val $fullEntityName = $initialization"
            }
        }

        addStatement(statement, compoundSpec.declaration.toClassName())
        return newMainEntitySpec
    }

    data class MainEntitySpec(
        val spec: EntitySpec,
        val property: CompoundPropertySpec,
        val parents: Set<CompoundPropertySpec>
    )

    private fun CodeBlock.Builder.addMainPropertyMapping(
        functionSpec: DaoFunctionSpec,
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
                    functionSpec,
                    mainPropertyType.spec,
                    isNested,
                    isForReturn = false,
                    isEntityQuerySkipped,
                    mainEntitySpec,
                    relationSpec,
                    newParents
                )
            }

            is DataTypeSpec.DataType.Entity -> {
                if (!isEntityQuerySkipped) {
                    val parameters = if (mainEntitySpec == null || relationSpec == null) {
                        functionSpec.getParametersCall()
                    } else {
                        val mainPropertyName = mainEntitySpec.parents.asName()
                        val columnAccess = mainEntitySpec.spec
                            .getColumnParameterAccess(relationSpec.relation.parentColumn)

                        "$mainPropertyName.$columnAccess"
                    }

                    val methodName = functionSpec.getCompoundFunctionName(newParents)
                    val awaitFunction = property.dataTypeSpec.getAwaitFunction()
                    addStatement("val $fullEntityName = $daoQueriesPropertyName.$methodName($parameters).$awaitFunction()")
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

    private fun CodeBlock.Builder.addRelationsMapping(
        functionSpec: DaoFunctionSpec,
        compoundSpec: CompoundSpec,
        mainEntitySpec: MainEntitySpec,
        parents: Set<CompoundPropertySpec>
    ) {
        compoundSpec.relations.forEach { compoundRelationSpec ->
            val property = compoundRelationSpec.property
            val newParents = parents + property
            val fullEntityName = newParents.asName()

            when (val dataType = property.dataTypeSpec.dataType) {
                is DataTypeSpec.DataType.Compound -> {
                    addCompoundMapping(
                        functionSpec,
                        dataType.spec,
                        isNested = false,
                        isForReturn = false,
                        isEntityQuerySkipped = false,
                        mainEntitySpec,
                        compoundRelationSpec,
                        newParents
                    )
                }

                is DataTypeSpec.DataType.Entity -> {
                    val mainPropertyName = mainEntitySpec.parents.asName()
                    val columnAccess = mainEntitySpec.spec
                        .getColumnParameterAccess(compoundRelationSpec.relation.parentColumn)

                    val methodName = functionSpec.getCompoundFunctionName(newParents)
                    val awaitFunction = property.dataTypeSpec.getAwaitFunction()
                    addStatement("val $fullEntityName = $daoQueriesPropertyName.$methodName($mainPropertyName.$columnAccess).$awaitFunction()")
                }

                is DataTypeSpec.DataType.Collection -> {
                    addCollectionMapping(
                        functionSpec,
                        compoundRelationSpec,
                        mainEntitySpec,
                        dataType.wrappedDeclaration,
                        newParents
                    )
                }

                is DataTypeSpec.DataType.Class,
                is DataTypeSpec.DataType.Stream -> {
                    logger.throwException(
                        "This type is not supported as a relation entity for compounds: ${compoundSpec.mainProperty.dataTypeSpec}",
                        compoundSpec.mainProperty.declaration
                    )
                }
            }
        }
    }

    private fun  CodeBlock.Builder.addCollectionMapping(
        functionSpec: DaoFunctionSpec,
        relationSpec: CompoundRelationSpec,
        mainEntitySpec: MainEntitySpec,
        wrappedType: DataTypeSpec,
        parents: Set<CompoundPropertySpec>
    ) {
        val property = relationSpec.property
        val newParents = parents + property
        val fullEntityName = newParents.asName()

        when (val dataType = wrappedType.dataType) {
            is DataTypeSpec.DataType.Compound -> {
                addCompoundRelationMapping(
                    functionSpec,
                    property,
                    dataType.spec.mainProperty,
                    mainEntitySpec,
                    relationSpec,
                    parents
                )

                addCompoundMapping(
                    functionSpec,
                    dataType.spec,
                    isNested = true,
                    isForReturn = true,
                    isEntityQuerySkipped = true,
                    mainEntitySpec,
                    relationSpec,
                    newParents
                )

                endControlFlow()
            }

            is DataTypeSpec.DataType.Entity -> {
                val methodName = functionSpec.getCompoundFunctionName(parents)
                val awaitFunction = property.dataTypeSpec.getAwaitFunction()

                val mainPropertyName = mainEntitySpec.parents.asName()
                val columnAccess = mainEntitySpec.spec
                    .getColumnParameterAccess(relationSpec.relation.parentColumn)

                addStatement("val $fullEntityName = $daoQueriesPropertyName.$methodName($mainPropertyName.$columnAccess).$awaitFunction()")
            }

            is DataTypeSpec.DataType.Collection -> {
                logger.throwException(
                    "Nested collections are not supported as a relation entity for compounds: ${property.dataTypeSpec}",
                    property.declaration
                )
            }

            is DataTypeSpec.DataType.Class,
            is DataTypeSpec.DataType.Stream -> {
                logger.throwException(
                    "This type is not supported as a relation entity for compounds: ${property.dataTypeSpec}",
                    property.declaration
                )
            }
        }
    }

    private fun CodeBlock.Builder.addCompoundRelationMapping(
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
        addStatement("val $propertyName = $functionCall")
        beginControlFlow("val $parentName = $propertyName.map { $propertyName -> ")
    }

    private fun CodeBlock.Builder.addCompoundPropertyMapping(
        functionSpec: DaoFunctionSpec,
        property: CompoundPropertySpec,
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

        val propertyName = newParents.asName()
        val functionCall = if (isEntityQuerySkipped) {
            propertyName
        } else {
            val functionName = functionSpec.getCompoundFunctionName(newParents)
            val parameters = functionSpec.getParametersCall()
            val actualAwaitFunction = awaitFunction ?: property.dataTypeSpec.getAwaitFunction()
            val functionCall = "$daoQueriesPropertyName.$functionName($parameters).$actualAwaitFunction()"
            if (isNested) {
                functionCall
            } else {
                "return $functionCall"
            }
        }

        beginControlFlow("$functionCall.map { $propertyName -> ")
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
