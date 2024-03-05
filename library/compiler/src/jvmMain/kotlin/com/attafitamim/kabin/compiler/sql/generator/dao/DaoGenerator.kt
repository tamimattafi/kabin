package com.attafitamim.kabin.compiler.sql.generator.dao

import com.attafitamim.kabin.compiler.sql.utils.poet.SYMBOL_ACCESS_SIGN
import com.attafitamim.kabin.compiler.sql.utils.poet.buildSpec
import com.attafitamim.kabin.compiler.sql.utils.poet.simpleNameString
import com.attafitamim.kabin.compiler.sql.utils.poet.toPascalCase
import com.attafitamim.kabin.compiler.sql.utils.poet.typeInitializer
import com.attafitamim.kabin.compiler.sql.utils.poet.writeType
import com.attafitamim.kabin.compiler.sql.utils.spec.getDataReturnType
import com.attafitamim.kabin.core.dao.KabinDao
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.column.ColumnTypeSpec
import com.attafitamim.kabin.specs.dao.DataTypeSpec
import com.attafitamim.kabin.specs.dao.DaoActionSpec
import com.attafitamim.kabin.specs.dao.DaoFunctionSpec
import com.attafitamim.kabin.specs.dao.DaoSpec
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
            val functionCodeBuilder = CodeBlock.builder()

            val returnType = functionSpec.returnTypeSpec
            if (functionSpec.transactionSpec != null) {
                if (returnType != null) {
                    functionCodeBuilder.beginControlFlow("return transactionWithResult")
                } else {
                    functionCodeBuilder.beginControlFlow("transaction")
                }
            }

            functionCodeBuilder.addQueryLogic(functionSpec,
                returnType
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

    private fun CodeBlock.Builder.addQueryLogic(
        functionSpec: DaoFunctionSpec,
        returnType: DataTypeSpec?
    ) {
        val returnTypeSpec = returnType?.getDataReturnType()
        val returnTypeDataType = returnTypeSpec?.dataType
        if (returnTypeDataType is DataTypeSpec.DataType.Compound) {
            addCompoundCreation(
                functionSpec, returnType,
                returnTypeDataType.spec
            )
        } else {
            addSimpleReturnLogic(
                functionSpec, returnType
            )
        }
    }

    private fun CodeBlock.Builder.addSimpleReturnLogic(
        functionSpec: DaoFunctionSpec,
        returnType: DataTypeSpec?
    ) {
        val functionName = functionSpec.declaration.simpleNameString
        val parameters = functionSpec.parameters.joinToString { parameter ->
            parameter.name
        }

        val awaitFunction = returnType?.getAwaitFunction()
        val functionCall = when (functionSpec.actionSpec) {
            is DaoActionSpec.Delete,
            is DaoActionSpec.Insert,
            is DaoActionSpec.Update,
            is DaoActionSpec.Upsert,
            is DaoActionSpec.Query,
            is DaoActionSpec.RawQuery -> {
                if (awaitFunction.isNullOrBlank()) {
                    "$daoQueriesPropertyName.$functionName($parameters)"
                } else {
                    "$daoQueriesPropertyName.$functionName($parameters).$awaitFunction()"
                }
            }

            null -> "super.$functionName($parameters)"
        }

        val actualFunctionCall = if (returnType != null && functionSpec.transactionSpec == null) {
            "return $functionCall"
        } else {
            functionCall
        }

        addStatement(actualFunctionCall)
    }

    private fun CodeBlock.Builder.addCompoundCreation(
        functionSpec: DaoFunctionSpec,
        returnType: DataTypeSpec,
        compoundSpec: CompoundSpec,
        isNested: Boolean = false
    ) {
        when (val type = returnType.dataType) {
            is DataTypeSpec.DataType.Wrapper -> {
                addStatement("// here we should get wrapper and start mapping { -> ")

                addStatement("// -> here we are inside the mapping, we go deeper")
                addCompoundCreation(
                    functionSpec,
                    type.wrappedDeclaration,
                    compoundSpec,
                    isNested = true
                )

                addStatement("// } here we finished mapping")
            }

            is DataTypeSpec.DataType.Compound -> {
                addStatement("// here we reached the base layer of compound and we should start mapping it")
                addCompoundMapping(
                    functionSpec,
                    compoundSpec
                )
            }

            is DataTypeSpec.DataType.Entity,
            is DataTypeSpec.DataType.Class -> error("not supported here")
        }
    }

    private fun CodeBlock.Builder.addCompoundMapping(
        functionSpec: DaoFunctionSpec,
        compoundSpec: CompoundSpec,
        mainEntitySpec: MainEntitySpec? = null,
        relationSpec: CompoundRelationSpec? = null,
        parents: Set<CompoundPropertySpec> = LinkedHashSet()
    ): MainEntitySpec {
        val newMainEntitySpec = addMainPropertyMapping(
            functionSpec,
            compoundSpec,
            mainEntitySpec,
            relationSpec,
            parents
        )

        addRelationsMapping(functionSpec, compoundSpec, newMainEntitySpec, parents)
        //addStatement("// here must new instance of compound $parent")

        val initialization = typeInitializer(
            isForReturn = parents.isEmpty()
        )

        if (parents.isNotEmpty()) {
            val fullEntityName = parents.joinToString("_") {
                it.declaration.simpleNameString
            }

            addStatement("val $fullEntityName = 123 ")
        } else {
            addStatement("// maybe return?")
            addStatement("123")
        }

        addStatement("// ----- ")

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
        mainEntitySpec: MainEntitySpec?,
        relationSpec: CompoundRelationSpec?,
        parents: Set<CompoundPropertySpec>
    ): MainEntitySpec {
        val property = compoundSpec.mainProperty
        val propertyName = property.declaration.simpleNameString
        val newParents = parents + property
        val fullEntityName = newParents.joinToString("_") {
            it.declaration.simpleNameString
        }

        return when (val mainPropertyType = property.dataTypeSpec.dataType) {
            is DataTypeSpec.DataType.Compound -> {
                addStatement("// $parents's main property $propertyName is a compound, we should go deeper ->")
                return addCompoundMapping(
                    functionSpec,
                    mainPropertyType.spec,
                    mainEntitySpec,
                    relationSpec,
                    newParents
                )
            }

            is DataTypeSpec.DataType.Entity -> {
                //addStatement("// $parent's main property $propertyName is an entity, we can stop here")
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
                    //addStatement("// $parent's relation property $propertyName is a compound, we should go deeper ->")
                    addCompoundMapping(
                        functionSpec,
                        dataType.spec,
                        mainEntitySpec,
                        compoundRelationSpec,
                        newParents
                    )
                }

                is DataTypeSpec.DataType.Entity -> {
                    //addStatement("// $parent's relation property $propertyName is an entity, we can stop here")
                    val mainPropertyName = mainEntitySpec.parents.asName()
                    val columnAccess = mainEntitySpec.spec
                        .getColumnParameterAccess(compoundRelationSpec.relation.parentColumn)

                    val methodName = functionSpec.getCompoundFunctionName(newParents)
                    val awaitFunction = property.dataTypeSpec.getAwaitFunction()
                    addStatement("val $fullEntityName = $daoQueriesPropertyName.$methodName($mainPropertyName.$columnAccess).$awaitFunction()")
                }

                is DataTypeSpec.DataType.Collection -> {
                    //addStatement("// $parent's relation property $propertyName is a collection, we must go deeper")
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
                //addStatement("// $parent's collection property $propertyName is a compound, we should go deeper ->")
                addStatement("// here we should get wrapper and start mapping { -> ")
                addCompoundMapping(
                    functionSpec,
                    dataType.spec,
                    mainEntitySpec,
                    relationSpec,
                    newParents
                )
            }

            is DataTypeSpec.DataType.Entity -> {
                //addStatement("// $parent's collection property $propertyName is an entity, we can stop here and start mapping")
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

    private fun CodeBlock.Builder.addCompoundPropertyDeclaration(
        functionName: String,
        parameters: String,
        property: CompoundPropertySpec,
        awaitFunction: String? = null
    ) {
        val propertyName = property.declaration.simpleNameString
        val actualAwaitFunction = awaitFunction ?: property.dataTypeSpec.getAwaitFunction()

        val queriesFunctionName = buildString {
            append(functionName, propertyName.toPascalCase())
        }

        val functionCall = "$daoQueriesPropertyName.$queriesFunctionName($parameters).$actualAwaitFunction()"
        addStatement("val $propertyName = $functionCall")
    }

    private fun CodeBlock.Builder.addCompoundPropertyMapping(
        functionName: String,
        parameters: String,
        property: CompoundPropertySpec,
        awaitFunction: String? = null,
        isNested: Boolean = false
    ) {
        val propertyName = property.declaration.simpleNameString

        val functionCall = if (isNested) {
            propertyName
        } else {
            val actualAwaitFunction = awaitFunction ?: property.dataTypeSpec.getAwaitFunction()

            val queriesFunctionName = buildString {
                append(functionName, propertyName.toPascalCase())
            }

            "return $daoQueriesPropertyName.$queriesFunctionName($parameters).$actualAwaitFunction()"
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

    private fun DaoFunctionSpec.getCompoundFunctionName(parents: Set<CompoundPropertySpec>) =
        buildString {
            append(declaration.simpleNameString)
            parents.forEach { compoundPropertySpec ->
                append(compoundPropertySpec.declaration.simpleNameString.toPascalCase())
            }
        }

    private fun DaoFunctionSpec.getParametersCall(): String = parameters.joinToString { parameter ->
        parameter.name
    }

    private fun Set<CompoundPropertySpec>.asName() = joinToString("_") {
        it.declaration.simpleNameString
    }

    private fun EntitySpec.getColumnParameterAccess(columnName: String): String {
        val chain = columns.getChainAccess(columnName)
        return chain.joinToString(SYMBOL_ACCESS_SIGN) {  columnSpec ->
            columnSpec.declaration.simpleNameString
        }
    }

    private fun ColumnSpec.getAccessChain(columnName: String): List<ColumnSpec> {
        val chain = ArrayList<ColumnSpec>()
        when (val dataType = typeSpec.dataType) {
            is ColumnTypeSpec.DataType.Class -> {
                if (name == columnName) {
                    chain.add(this)
                }
            }

            is ColumnTypeSpec.DataType.Embedded -> {
                val newChain = dataType.columns.getChainAccess(columnName)
                chain.addAll(newChain)
            }
        }

        return chain
    }

    private fun List<ColumnSpec>.getChainAccess(columnName: String): List<ColumnSpec> {
        forEach { columnSpec ->
            val chain = columnSpec.getAccessChain(columnName)
            if (chain.isNotEmpty()) {
                return chain
            }
        }

        return emptyList()
    }

    private fun CompoundPropertySpec.getMainEntitySpec() {
        /*val parents = LinkedHashSet<CompoundPropertySpec>()

        var currentType = this
        var currentTypeDataType = currentType.dataTypeSpec.dataType

        while (currentTypeDataType !is DataTypeSpec.DataType.Entity) {
            parents.add(this)

            when (currentTypeDataType) {
                is DataTypeSpec.DataType.Compound -> {

                }

                is DataTypeSpec.DataType.Collection -> {

                }

                is DataTypeSpec.DataType.Class,
                is DataTypeSpec.DataType.Stream,
                is DataTypeSpec.DataType.Entity -> {
                    logger.throwException("Not supported here", currentType.declaration)
                }
            }

            currentType = currentTypeDataType.wrappedDeclaration
            currentTypeDataType = currentType.dataType
        }

        return currentType*/
    }

    data class Result(
        val className: ClassName
    )
}
