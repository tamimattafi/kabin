package com.attafitamim.kabin.compiler.sql.generator.queries

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.Query
import app.cash.sqldelight.SuspendingTransacterImpl
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.compiler.sql.generator.references.ColumnAdapterReference
import com.attafitamim.kabin.compiler.sql.generator.references.MapperReference
import com.attafitamim.kabin.compiler.sql.syntax.SQLDaoQuery
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.VALUE
import com.attafitamim.kabin.compiler.sql.utils.poet.DRIVER_NAME
import com.attafitamim.kabin.compiler.sql.utils.poet.SYMBOL_ACCESS_SIGN
import com.attafitamim.kabin.compiler.sql.utils.poet.asSpecs
import com.attafitamim.kabin.compiler.sql.utils.poet.buildSpec
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.getAdapterReference
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.supportedBinders
import com.attafitamim.kabin.compiler.sql.utils.poet.references.getPropertyName
import com.attafitamim.kabin.compiler.sql.utils.poet.simpleNameString
import com.attafitamim.kabin.compiler.sql.utils.poet.sqldelight.addDriverExecutionCode
import com.attafitamim.kabin.compiler.sql.utils.poet.sqldelight.addDriverQueryCode
import com.attafitamim.kabin.compiler.sql.utils.poet.toPascalCase
import com.attafitamim.kabin.compiler.sql.utils.poet.toCamelCase
import com.attafitamim.kabin.compiler.sql.utils.poet.typeInitializer
import com.attafitamim.kabin.compiler.sql.utils.poet.writeType
import com.attafitamim.kabin.compiler.sql.utils.spec.getDataReturnType
import com.attafitamim.kabin.compiler.sql.utils.spec.getQueryClassName
import com.attafitamim.kabin.compiler.sql.utils.sql.buildSQLQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.dao.getFlatColumns
import com.attafitamim.kabin.compiler.sql.utils.sql.dao.getSQLQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.dao.getSelectSQLQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.sqlType
import com.attafitamim.kabin.core.table.KabinMapper
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.utils.resolveClassDeclaration
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.column.ColumnTypeSpec
import com.attafitamim.kabin.specs.dao.DaoActionSpec
import com.attafitamim.kabin.specs.dao.DaoFunctionSpec
import com.attafitamim.kabin.specs.dao.DaoParameterSpec
import com.attafitamim.kabin.specs.dao.DaoSpec
import com.attafitamim.kabin.specs.dao.DataTypeSpec
import com.attafitamim.kabin.specs.relation.compound.CompoundPropertySpec
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

class QueriesGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: KabinOptions
) {

    fun generate(daoSpec: DaoSpec): Result {
        val className = daoSpec.getQueryClassName(options)
        val superClassName = SuspendingTransacterImpl::class.asClassName()

        val classBuilder = TypeSpec.classBuilder(className)
            .superclass(superClassName)
            .addSuperclassConstructorParameter(DRIVER_NAME)

        val adapters = HashSet<ColumnAdapterReference>()
        val mappers = HashSet<MapperReference>()

        daoSpec.functionSpecs.forEach { functionSpec ->
            val actionSpec = functionSpec.actionSpec
            if (actionSpec != null) {
                val returnTypeSpec = functionSpec.returnTypeSpec
                if (returnTypeSpec == null) {
                    val requiredAdapters = classBuilder.addVoidQueryFunction(
                        functionSpec,
                        actionSpec
                    )

                    adapters.addAll(requiredAdapters)
                } else {
                    val result = classBuilder.addResultQuery(
                        className,
                        functionSpec,
                        actionSpec,
                        returnTypeSpec,
                        functionSpec.declaration.simpleNameString
                    )

                    adapters.addAll(result.adapters)
                    mappers.addAll(result.mappers)
                }
            }
        }

        val constructorBuilder = FunSpec.constructorBuilder()
            .addParameter(DRIVER_NAME, SqlDriver::class.asClassName())

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

        mappers.forEach { mapper ->
            val propertyName = mapper.getPropertyName(options)
            val adapterType = KabinMapper::class.asClassName()
                .parameterizedBy(mapper.returnType)

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

        classBuilder.primaryConstructor(constructorBuilder.build())

        codeGenerator.writeType(
            className,
            classBuilder.build()
        )

        return Result(
            className,
            adapters,
            mappers
        )
    }

    private fun TypeSpec.Builder.addResultQuery(
        queriesClassName: ClassName,
        functionSpec: DaoFunctionSpec,
        actionSpec: DaoActionSpec,
        returnTypeSpec: DataTypeSpec,
        parentName: String
    ): Result {
        val adapters = HashSet<ColumnAdapterReference>()
        val mappers = HashSet<MapperReference>()

        val dataReturnType = returnTypeSpec.getDataReturnType()
        val camelCaseName = parentName.toPascalCase()
        val className = when (val dataType = dataReturnType.dataType as DataTypeSpec.DataType.Data) {
            is DataTypeSpec.DataType.Class,
            is DataTypeSpec.DataType.Entity -> {
                val result = addResultQueryClass(
                    queriesClassName,
                    functionSpec,
                    actionSpec,
                    returnTypeSpec,
                    camelCaseName
                )

                adapters.addAll(result.adapters)
                mappers.addAll(result.mappers)

                val requiredAdapters = addResultQueryFunction(
                    result.className,
                    functionSpec.parameters,
                    parentName,
                    returnTypeSpec
                )

                adapters.addAll(requiredAdapters)
                result.className
            }

            is DataTypeSpec.DataType.Compound -> {
                val mainProperty = dataType.spec.mainProperty

                val result = addCompoundMainEntityQuery(
                    queriesClassName,
                    functionSpec,
                    actionSpec,
                    mainProperty,
                    parentName
                )

                adapters.addAll(result.adapters)
                mappers.addAll(result.mappers)

                dataType.spec.relations.forEach { compoundRelationSpec ->
                    val parentColumn = mainProperty.dataTypeSpec
                        .getEntityColumn(compoundRelationSpec.relation.parentColumn)

                    val entityColumn = compoundRelationSpec.property.dataTypeSpec
                        .getEntityColumn(compoundRelationSpec.relation.entityColumn)

                    val relationResult = addCompoundPropertyQuery(
                        queriesClassName,
                        parentColumn,
                        entityColumn,
                        compoundRelationSpec.property,
                        parentName
                    )

                    adapters.addAll(relationResult.adapters)
                    mappers.addAll(relationResult.mappers)
                }

                result.className
            }
        }

        return Result(
            className,
            adapters,
            mappers
        )
    }

    private fun DataTypeSpec.getEntityColumn(name: String): ColumnSpec =
        when (val type = dataType) {
            is DataTypeSpec.DataType.Entity -> getFlatColumns(type.spec.columns).first { columnSpec ->
                columnSpec.name == name
            }

            is DataTypeSpec.DataType.Compound -> {
                type.spec.mainProperty.dataTypeSpec.getEntityColumn(name)
            }

            is DataTypeSpec.DataType.Collection -> {
                type.wrappedDeclaration.getEntityColumn(name)
            }

            is DataTypeSpec.DataType.Stream,
            is DataTypeSpec.DataType.Class -> error("not supported here")
        }

    private fun TypeSpec.Builder.addCompoundMainEntityQuery(
        queriesClassName: ClassName,
        functionSpec: DaoFunctionSpec,
        actionSpec: DaoActionSpec,
        propertySpec: CompoundPropertySpec,
        parent: String
    ): Result {
        val adapters = HashSet<ColumnAdapterReference>()
        val mappers = HashSet<MapperReference>()

        val camelCaseName = parent.toPascalCase()
        val newName = buildString {
            append(camelCaseName)
            append(propertySpec.declaration.simpleNameString.toPascalCase())
        }

        val result = when (val dataType = propertySpec.dataTypeSpec.dataType) {
            is DataTypeSpec.DataType.Entity -> {
                val result = addResultQueryClass(
                    queriesClassName,
                    functionSpec,
                    actionSpec,
                    propertySpec.dataTypeSpec,
                    newName
                )

                adapters.addAll(result.adapters)
                mappers.addAll(result.mappers)

                val requiredAdapters = addResultQueryFunction(
                    result.className,
                    functionSpec.parameters,
                    newName.toCamelCase(),
                    propertySpec.dataTypeSpec
                )

                adapters.addAll(requiredAdapters)
                result
            }

            is DataTypeSpec.DataType.Compound -> {
                val mainProperty = dataType.spec.mainProperty
                val result = addCompoundMainEntityQuery(
                    queriesClassName,
                    functionSpec,
                    actionSpec,
                    mainProperty,
                    newName
                )

                adapters.addAll(result.adapters)
                mappers.addAll(result.mappers)

                dataType.spec.relations.forEach { compoundRelationSpec ->
                    val parentColumn = mainProperty.dataTypeSpec
                        .getEntityColumn(compoundRelationSpec.relation.parentColumn)

                    val entityColumn = compoundRelationSpec.property.dataTypeSpec
                        .getEntityColumn(compoundRelationSpec.relation.entityColumn)

                    val relationResult = addCompoundPropertyQuery(
                        queriesClassName,
                        parentColumn,
                        entityColumn,
                        compoundRelationSpec.property,
                        newName
                    )

                    adapters.addAll(relationResult.adapters)
                    mappers.addAll(relationResult.mappers)
                }

                result
            }

            is DataTypeSpec.DataType.Class,
            is DataTypeSpec.DataType.Collection,
            is DataTypeSpec.DataType.Stream -> error("not supported")
        }

        return Result(
            result.className,
            adapters,
            mappers
        )
    }

    private fun TypeSpec.Builder.addCompoundPropertyQuery(
        queriesClassName: ClassName,
        parentColumn: ColumnSpec,
        entityColumn: ColumnSpec,
        propertySpec: CompoundPropertySpec,
        parent: String
    ): Result {
        val adapters = HashSet<ColumnAdapterReference>()
        val mappers = HashSet<MapperReference>()

        val camelCaseName = parent.toPascalCase()
        val newName = buildString {
            append(camelCaseName)
            append(propertySpec.declaration.simpleNameString.toPascalCase())
        }

        val result = addCompoundResultQueryClass(
            queriesClassName,
            parentColumn,
            entityColumn,
            propertySpec.dataTypeSpec,
            newName
        )

        adapters.addAll(result.adapters)
        mappers.addAll(result.mappers)

        return Result(
            result.className,
            adapters,
            mappers
        )
    }

    private fun TypeSpec.Builder.addVoidQueryFunction(
        daoFunctionSpec: DaoFunctionSpec,
        actionSpec: DaoActionSpec
    ): Set<ColumnAdapterReference> {
        val adapters = HashSet<ColumnAdapterReference>()

        val builder = FunSpec.builder(daoFunctionSpec.declaration.simpleName.asString())
            .addModifiers(KModifier.SUSPEND)
            .addParameters(daoFunctionSpec.declaration.parameters.asSpecs())

        when (actionSpec) {
            is DaoActionSpec.EntityAction -> {
                val requiredAdapters = builder.addEntityActionQuery(
                    daoFunctionSpec,
                    actionSpec
                )

                adapters.addAll(requiredAdapters)
            }

            is DaoActionSpec.Query -> {
                val query = actionSpec.getSQLQuery(daoFunctionSpec)
                val codeBlockBuilder = CodeBlock.builder()
                    .addStatement("driver.execute(")
                    .addStatement("${query.hashCode()},")
                    .addStatement("%P,", query.value)
                    .addStatement(query.parameters.size.toString())
                    .addStatement(")")

                val parameterAdapters = codeBlockBuilder.addQueryParametersBinding(
                    query.parameters
                )

                adapters.addAll(parameterAdapters)

                codeBlockBuilder.addStatement(".await()")
                builder.addCode(codeBlockBuilder.build())
            }

            is DaoActionSpec.RawQuery -> {
                val rawQuery = daoFunctionSpec.parameters.first().name
                val codeBlock = CodeBlock.builder()
                    .addStatement("driver.execute(")
                    .addStatement("${rawQuery}.hashCode(),")
                    .addStatement("$rawQuery,")
                    .addStatement("0")
                    .addStatement(")")
                    .addStatement(".await()")
                    .build()

                builder.addCode(codeBlock)
            }
        }

        addFunction(builder.build())
        return adapters
    }

    private fun FunSpec.Builder.addEntityActionQuery(
        daoFunctionSpec: DaoFunctionSpec,
        actionSpec: DaoActionSpec.EntityAction
    ): Set<ColumnAdapterReference> {
        val adapters = HashSet<ColumnAdapterReference>()
        daoFunctionSpec.parameters.forEach { parameter ->
            val requiredAdapters = addParameterEntityActionQuery(
                daoFunctionSpec,
                parameter,
                actionSpec,
                parameter.typeSpec
            )

            adapters.addAll(requiredAdapters)
        }

        return adapters
    }

    private fun FunSpec.Builder.addParameterEntityActionQuery(
        daoFunctionSpec: DaoFunctionSpec,
        daoParameterSpec: DaoParameterSpec,
        actionSpec: DaoActionSpec.EntityAction,
        dataTypeSpec: DataTypeSpec,
        name: String? = null
    ): Set<ColumnAdapterReference> {
        val adapters = HashSet<ColumnAdapterReference>()
        val parameterName = name ?: daoParameterSpec.name

        if (dataTypeSpec.isNullable) {
            beginControlFlow("$parameterName?.let {")
        }

        when (val dataType = dataTypeSpec.dataType) {
            is DataTypeSpec.DataType.Class,
            is DataTypeSpec.DataType.Stream -> {
                val annotationName = actionSpec.javaClass.simpleName
                logger.throwException(
                    "Only entities are allowed as parameters for $annotationName functions",
                    daoFunctionSpec.declaration
                )
            }

            is DataTypeSpec.DataType.Entity -> {
                val query = actionSpec.getSQLQuery(dataType.spec)
                addDriverExecutionCode(
                    query.hashCode(),
                    query.value,
                    query.parametersSize
                ) {
                    val requiredAdapters = addQueryEntityBinding(
                        parameterName,
                        query.columns
                    )

                    adapters.addAll(requiredAdapters)
                }

                addStatement("""
                notifyQueries(${query.hashCode()}) { emit ->
                    emit(%S)
                }
                """.trimIndent(), dataType.spec.tableName)
            }

            is DataTypeSpec.DataType.Compound -> {
                dataType.spec.mainProperty.let { mainProperty ->
                    val propertyAccess = buildString {
                        append(
                            parameterName,
                            SYMBOL_ACCESS_SIGN,
                            mainProperty.declaration.simpleNameString
                        )
                    }

                    val requiredAdapters = addParameterEntityActionQuery(
                        daoFunctionSpec,
                        daoParameterSpec,
                        actionSpec,
                        mainProperty.dataTypeSpec,
                        propertyAccess
                    )

                    adapters.addAll(requiredAdapters)
                }

                dataType.spec.relations.forEach { compoundRelationSpec ->
                    val propertyAccess = buildString {
                        append(
                            parameterName,
                            SYMBOL_ACCESS_SIGN,
                            compoundRelationSpec.property.declaration.simpleNameString
                        )
                    }

                    val requiredAdapters = addParameterEntityActionQuery(
                        daoFunctionSpec,
                        daoParameterSpec,
                        actionSpec,
                        compoundRelationSpec.property.dataTypeSpec,
                        propertyAccess
                    )

                    adapters.addAll(requiredAdapters)
                }
            }

            is DataTypeSpec.DataType.Collection -> {
                val childName = buildString {
                    append(daoParameterSpec.name, "Child")
                }

                beginControlFlow("$parameterName.forEach { $childName ->")
                val requiredAdapters = addParameterEntityActionQuery(
                    daoFunctionSpec,
                    daoParameterSpec,
                    actionSpec,
                    dataType.wrappedDeclaration,
                    childName
                )

                endControlFlow()
                adapters.addAll(requiredAdapters)
            }
        }

        if (dataTypeSpec.isNullable) {
            endControlFlow()
        }

        return adapters
    }

    private fun TypeSpec.Builder.addResultQueryClass(
        queriesClassName: ClassName,
        functionSpec: DaoFunctionSpec,
        actionSpec: DaoActionSpec,
        returnTypeSpec: DataTypeSpec,
        name: String
    ): Result {
        val adapters = LinkedHashSet<ColumnAdapterReference>()
        val mappers = LinkedHashSet<MapperReference>()

        val returnType = returnTypeSpec.getDataReturnType().declaration.toClassName()
        val superClass = Query::class.asClassName()
            .parameterizedBy(returnType)

        val constructorBuilder = FunSpec.constructorBuilder()
        val queryClassName = ClassName(
            queriesClassName.packageName,
            queriesClassName.simpleName,
            name
        )

        val queryBuilder = TypeSpec.classBuilder(queryClassName)
            .superclass(superClass).addModifiers(KModifier.INNER)

        functionSpec.parameters.forEach { parameterSpec ->
            val parameterClassName = parameterSpec.declaration.type.toTypeName()
            constructorBuilder.addParameter(
                parameterSpec.name,
                parameterClassName
            )

            val propertySpec = PropertySpec.builder(
                parameterSpec.name,
                parameterClassName,
                KModifier.PRIVATE
            ).initializer(parameterSpec.name).build()
            queryBuilder.addProperty(propertySpec)
        }

        val addListenerFunction = Query<*>::addListener.buildSpec()
            .addModifiers(KModifier.OVERRIDE)
            .addListenerLogic(returnTypeSpec, SqlDriver::addListener.name)
            .build()

        val removeListenerFunction = Query<*>::removeListener.buildSpec()
            .addModifiers(KModifier.OVERRIDE)
            .addListenerLogic(returnTypeSpec, SqlDriver::removeListener.name)
            .build()

        val typeName = TypeVariableName.invoke("R")
        val queryResultType = QueryResult::class.asClassName().parameterizedBy(typeName)
        val mapperParameterType = LambdaTypeName.get(
            SqlCursor::class.asTypeName(),
            returnType = queryResultType,
        )

        val executeFunctionBuilder = FunSpec.builder("execute")
            .addTypeVariable(typeName)
            .returns(queryResultType)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("mapper", mapperParameterType)

        val mapperReference = MapperReference(returnType)
        val mapperName = mapperReference.getPropertyName(options)
        queryBuilder.primaryConstructor(constructorBuilder.build())
            .addSuperclassConstructorParameter("$mapperName::map")

        mappers.add(mapperReference)

        when (actionSpec) {
            is DaoActionSpec.EntityAction -> {
                functionSpec.parameters.forEach { entityParameter ->
                    val dataTypeSpec = entityParameter.typeSpec.dataType as DataTypeSpec.DataType.Entity
                    val query = actionSpec.getSQLQuery(dataTypeSpec.spec)

                    executeFunctionBuilder.addDriverQueryCode(
                        query.hashCode(),
                        query.value,
                        query.parametersSize
                    ) {
                        val bindingAdapters = addQueryEntityBinding(
                            entityParameter.name,
                            query.columns
                        )

                        adapters.addAll(bindingAdapters)
                    }
                }
            }

            is DaoActionSpec.Query -> {
                val query = actionSpec.getSQLQuery(functionSpec)
                executeFunctionBuilder.addDriverQueryCode(query) {
                    val bindingAdapters = addQueryParametersBinding(query.parameters)
                    adapters.addAll(bindingAdapters)
                }
            }

            is DaoActionSpec.RawQuery -> {
                val rawQuery = functionSpec.parameters.first().name
                executeFunctionBuilder.addDriverQueryCode(
                    "${rawQuery}.hashCode()",
                    rawQuery,
                    "0"
                )
            }
        }

        queryBuilder
            .addFunction(addListenerFunction)
            .addFunction(removeListenerFunction)
            .addFunction(executeFunctionBuilder.build())

        addType(queryBuilder.build())

        return Result(
            queryClassName,
            adapters,
            mappers
        )
    }

    private fun TypeSpec.Builder.addCompoundResultQueryClass(
        queriesClassName: ClassName,
        parentColumn: ColumnSpec,
        entityColumn: ColumnSpec,
        returnTypeSpec: DataTypeSpec,
        name: String
    ): Result {
        val adapters = LinkedHashSet<ColumnAdapterReference>()
        val mappers = LinkedHashSet<MapperReference>()

        val returnDataTypeSpec = returnTypeSpec.getDataReturnType()
        val queryClassName = when (val type = returnDataTypeSpec.dataType as DataTypeSpec.DataType.Data) {
            is DataTypeSpec.DataType.Class -> error("not supported here")
            is DataTypeSpec.DataType.Compound -> {
                val mainProperty = type.spec.mainProperty
                val newName = buildString {
                    append(name, mainProperty.declaration.simpleNameString.toPascalCase())
                }

                val result = addCompoundResultQueryClass(
                    queriesClassName,
                    parentColumn,
                    entityColumn,
                    type.spec.mainProperty.dataTypeSpec,
                    newName
                )

                adapters.addAll(result.adapters)
                mappers.addAll(result.mappers)

                type.spec.relations.forEach { compoundRelationSpec ->
                    val relationName = buildString {
                        append(
                            name,
                            compoundRelationSpec.property.declaration.simpleNameString.toPascalCase()
                        )
                    }

                    val relationResult = addCompoundResultQueryClass(
                        queriesClassName,
                        parentColumn,
                        entityColumn,
                        compoundRelationSpec.property.dataTypeSpec,
                        relationName
                    )

                    adapters.addAll(relationResult.adapters)
                    mappers.addAll(relationResult.mappers)
                }

                result.className
            }

            is DataTypeSpec.DataType.Entity -> {
                val returnType = type.spec.declaration.toClassName()
                val superClass = Query::class.asClassName().parameterizedBy(returnType)

                val constructorBuilder = FunSpec.constructorBuilder()
                val queryClassName = ClassName(
                    queriesClassName.packageName,
                    queriesClassName.simpleName,
                    name
                )

                val queryBuilder = TypeSpec.classBuilder(queryClassName)
                    .superclass(superClass)
                    .addModifiers(KModifier.INNER)

                val parameterClassName = parentColumn.declaration.type.toTypeName()
                constructorBuilder.addParameter(
                    parentColumn.declaration.simpleNameString,
                    parameterClassName
                )

                val propertySpec = PropertySpec.builder(
                    parentColumn.declaration.simpleNameString,
                    parameterClassName,
                    KModifier.PRIVATE
                ).initializer(parentColumn.declaration.simpleNameString).build()
                queryBuilder.addProperty(propertySpec)

                val addListenerFunction = Query<*>::addListener.buildSpec()
                    .addModifiers(KModifier.OVERRIDE)
                    .addListenerLogic(returnTypeSpec, SqlDriver::addListener.name)
                    .build()

                val removeListenerFunction = Query<*>::removeListener.buildSpec()
                    .addModifiers(KModifier.OVERRIDE)
                    .addListenerLogic(returnTypeSpec, SqlDriver::removeListener.name)
                    .build()

                val typeName = TypeVariableName.invoke("R")
                val queryResultType = QueryResult::class.asClassName()
                    .parameterizedBy(typeName)

                val mapperParameterType = LambdaTypeName.get(
                    SqlCursor::class.asTypeName(),
                    returnType = queryResultType,
                )

                val executeFunctionBuilder = FunSpec.builder("execute")
                    .addTypeVariable(typeName)
                    .returns(queryResultType)
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("mapper", mapperParameterType)

                val mapperReference = MapperReference(returnType)
                val mapperName = mapperReference.getPropertyName(options)
                queryBuilder.primaryConstructor(constructorBuilder.build())
                    .addSuperclassConstructorParameter("$mapperName::map")

                mappers.add(mapperReference)

                val query = getSelectSQLQuery(type.spec, entityColumn)
                executeFunctionBuilder.addDriverQueryCode(
                    query.hashCode(),
                    query.value,
                    query.parametersSize
                ) {
                    beginControlFlow("")
                    val requiredAdapters = addQueryColumnSpecBinding(
                        parentColumn,
                        "0"
                    )

                    adapters.addAll(requiredAdapters)
                    endControlFlow()
                }

                queryBuilder
                    .addFunction(addListenerFunction)
                    .addFunction(removeListenerFunction)
                    .addFunction(executeFunctionBuilder.build())

                addType(queryBuilder.build())

                val requiredAdapters = addCompoundResultQueryFunction(
                    queryClassName,
                    parentColumn,
                    name.toCamelCase(),
                    returnTypeSpec
                )

                adapters.addAll(requiredAdapters)
                queryClassName
            }
        }

        return Result(
            queryClassName,
            adapters,
            mappers
        )
    }

    private fun TypeSpec.Builder.addResultQueryFunction(
        queryClassName: ClassName,
        parameters: List<DaoParameterSpec>,
        name: String,
        returnTypeSpec: DataTypeSpec
    ): Set<ColumnAdapterReference> {
        val adapters = HashSet<ColumnAdapterReference>()

        val returnType = returnTypeSpec.getDataReturnType().declaration.toClassName()
        val parameterNames = ArrayList<String>(parameters.size)
        val parametersSpec = parameters.map { daoParameterSpec ->
            parameterNames.add(daoParameterSpec.name)
            daoParameterSpec.declaration.buildSpec().build()
        }

        val builder = FunSpec.builder(name)
            .addParameters(parametersSpec)

        val queryReturnType = Query::class.asClassName()
            .parameterizedBy(returnType)

        builder.returns(queryReturnType)

        val queryReturnBlock = CodeBlock.builder()
        val typeInitializer = typeInitializer(
            parameterNames,
            isForReturn = true
        )

        queryReturnBlock.addStatement(
            typeInitializer,
            queryClassName
        )

        builder.addCode(queryReturnBlock.build())

        addFunction(builder.build())
        return adapters
    }


    private fun TypeSpec.Builder.addCompoundResultQueryFunction(
        queryClassName: ClassName,
        parentColumn: ColumnSpec,
        name: String,
        returnTypeSpec: DataTypeSpec
    ): Set<ColumnAdapterReference> {
        val adapters = HashSet<ColumnAdapterReference>()

        val returnType = returnTypeSpec.getDataReturnType().declaration.toClassName()
        val parametersSpec = ParameterSpec.builder(
            parentColumn.declaration.simpleNameString,
            parentColumn.declaration.type.toTypeName()
        ).build()

        val builder = FunSpec.builder(name)
            .addParameter(parametersSpec)

        val queryReturnType = Query::class.asClassName()
            .parameterizedBy(returnType)

        builder.returns(queryReturnType)

        val queryReturnBlock = CodeBlock.builder()
        val typeInitializer = typeInitializer(
            listOf(parentColumn.declaration.simpleNameString),
            isForReturn = true
        )

        queryReturnBlock.addStatement(
            typeInitializer,
            queryClassName
        )

        builder.addCode(queryReturnBlock.build())

        addFunction(builder.build())
        return adapters
    }

    private fun FunSpec.Builder.addListenerLogic(
        returnType: DataTypeSpec?,
        listenerMethod: String
    ): FunSpec.Builder = apply {
        when (val type = returnType?.dataType) {
            null,
            is DataTypeSpec.DataType.Class,
            is DataTypeSpec.DataType.Compound -> return@apply

            is DataTypeSpec.DataType.Entity -> {
                val driverName = DRIVER_NAME
                addStatement(
                    "$driverName.$listenerMethod(%S, listener = listener)",
                    type.spec.tableName
                )
            }

            is DataTypeSpec.DataType.Stream -> {
                return addListenerLogic(type.wrappedDeclaration, listenerMethod)
            }

            is DataTypeSpec.DataType.Collection -> {
                return addListenerLogic(type.wrappedDeclaration, listenerMethod)
            }
        }
    }

    private fun CodeBlock.Builder.addQueryEntityBinding(
        parent: String,
        columns: Collection<ColumnSpec>
    ): Set<ColumnAdapterReference> {
        if (columns.isEmpty()) {
            return emptySet()
        }

        beginControlFlow("")
        val adapters = addQueryEntityColumnsBinding(
            parent,
            columns
        )

        endControlFlow()
        return adapters
    }

    private fun CodeBlock.Builder.addQueryEntityColumnsBinding(
        parent: String,
        columns: Collection<ColumnSpec>,
        initialIndex: Int = 0,
        isParentNullable: Boolean = false
    ): Set<ColumnAdapterReference> {
        val adapters = HashSet<ColumnAdapterReference>()

        var currentIndex = initialIndex
        columns.forEach { columnSpec ->
            val isNullable = isParentNullable || columnSpec.typeSpec.isNullable
            val propertyName = columnSpec.declaration.simpleName.asString()
            val propertyAccess = buildString {
                append(parent)

                if (isParentNullable) {
                    append("?")
                }

                append(
                    SYMBOL_ACCESS_SIGN,
                    propertyName
                )
            }

            when (val dataType = columnSpec.typeSpec.dataType) {
                is ColumnTypeSpec.DataType.Class -> {
                    val adapter = addQueryParameterBinding(
                        isNullable,
                        propertyAccess,
                        currentIndex.toString(),
                        columnSpec.typeAffinity,
                        columnSpec.declaration.type.resolveClassDeclaration()
                    )

                    if (adapter != null) {
                        adapters.add(adapter)
                    }

                    currentIndex++
                }

                is ColumnTypeSpec.DataType.Embedded -> {
                    val requiredAdapters = addQueryEntityColumnsBinding(
                        propertyAccess,
                        dataType.columns,
                        currentIndex,
                        isNullable
                    )

                    adapters.addAll(requiredAdapters)
                    currentIndex += dataType.columns.size
                }
            }
        }

        return adapters
    }

    private fun CodeBlock.Builder.addQueryParametersBinding(
        parameterSpecs: List<DaoParameterSpec>,
    ): Set<ColumnAdapterReference> {
        if (parameterSpecs.isEmpty()) {
            return emptySet()
        }

        val adapters = HashSet<ColumnAdapterReference>()
        beginControlFlow("")

        var previousSimpleParametersCount = 0
        val previousDynamicParameters = ArrayList<DaoParameterSpec>()
        parameterSpecs.forEach { parameterSpec ->
            val dynamicSizes = previousDynamicParameters.joinToString(" + ") {
                if (it.typeSpec.isNullable) {
                    "${it.name}.orEmpty().size"
                } else {
                    "${it.name}.size"
                }
            }

            val indexExpression = if (dynamicSizes.isBlank()) {
                previousSimpleParametersCount.toString()
            } else if (previousSimpleParametersCount == 0) {
                dynamicSizes
            } else {
                "$dynamicSizes + $previousSimpleParametersCount"
            }

            val requiredAdapters = addQueryParameterSpecBinding(
                parameterSpec.name,
                parameterSpec.typeSpec,
                indexExpression
            )

            adapters.addAll(requiredAdapters)

            when (parameterSpec.typeSpec.dataType) {
                is DataTypeSpec.DataType.Entity,
                is DataTypeSpec.DataType.Stream,
                is DataTypeSpec.DataType.Compound -> error("not supported here")
                is DataTypeSpec.DataType.Collection -> {
                    previousDynamicParameters.add(parameterSpec)
                }

                is DataTypeSpec.DataType.Class -> {
                    previousSimpleParametersCount++
                }
            }
        }

        endControlFlow()
        return adapters
    }

    private fun CodeBlock.Builder.addQueryParameterSpecBinding(
        parameterName: String,
        dataTypeSpec: DataTypeSpec,
        index: String,
        name: String? = null
    ): Set<ColumnAdapterReference> {
        val adapters = HashSet<ColumnAdapterReference>()
        val actualParameterName = name ?: parameterName

        if (dataTypeSpec.isNullable) {
            beginControlFlow("$actualParameterName?.let {")
        }

        when (val dataType = dataTypeSpec.dataType) {
            is DataTypeSpec.DataType.Entity,
            is DataTypeSpec.DataType.Compound,
            is DataTypeSpec.DataType.Stream -> logger.throwException(
                "Only primitive values are allowed as query parameters"
            )

            is DataTypeSpec.DataType.Class -> {
                val adapter = addQueryParameterBinding(
                    dataTypeSpec.isNullable,
                    actualParameterName,
                    index,
                    dataTypeSpec.declaration.sqlType,
                    dataTypeSpec.declaration
                )

                if (adapter != null) {
                    adapters.add(adapter)
                }
            }

            is DataTypeSpec.DataType.Collection -> {
                val childName = buildString {
                    append(actualParameterName, "Child")
                }

                beginControlFlow("$actualParameterName.forEachIndexed { index, $childName ->")
                val indexExpression = if (index == "0") {
                    "index"
                } else {
                    "index + $index"
                }

                val requiredAdapters = addQueryParameterSpecBinding(
                    actualParameterName,
                    dataType.wrappedDeclaration,
                    indexExpression,
                    childName
                )

                endControlFlow()
                adapters.addAll(requiredAdapters)
            }
        }

        if (dataTypeSpec.isNullable) {
            endControlFlow()
        }

        return adapters
    }

    private fun CodeBlock.Builder.addQueryColumnSpecBinding(
        column: ColumnSpec,
        index: String,
        name: String? = null
    ): Set<ColumnAdapterReference> {
        val adapters = HashSet<ColumnAdapterReference>()
        val actualParameterName = name ?: column.declaration.simpleNameString

        val adapter = addQueryParameterBinding(
            column.typeSpec.isNullable,
            actualParameterName,
            index,
            column.typeAffinity,
            column.typeSpec.declaration
        )

        if (adapter != null) {
            adapters.add(adapter)
        }

        return adapters
    }

    private fun CodeBlock.Builder.addQueryParameterBinding(
        isNullable: Boolean,
        parameter: String,
        index: String,
        typeAffinity: ColumnInfo.TypeAffinity?,
        typeDeclaration: KSClassDeclaration
    ): ColumnAdapterReference? {
        val declarationAffinity = typeDeclaration.sqlType
        val actualTypeAffinity = typeAffinity ?: declarationAffinity

        val adapter = typeDeclaration.getAdapterReference(typeAffinity)
        val (actualParameter, bindFunction) = if (adapter != null) {
            val adapterName = adapter.getPropertyName()
            val encodeMethod = ColumnAdapter<*, *>::encode.name
            val encodeParameter = if (isNullable) {
                "$parameter?.let($adapterName::$encodeMethod)"
            } else {
                "$adapterName.$encodeMethod($parameter)"
            }

            encodeParameter to actualTypeAffinity.getBindFunction()
        } else {
            parameter to supportedBinders.getValue(typeDeclaration.qualifiedName?.asString())
        }

        addStatement("$bindFunction($index, $actualParameter)")
        return adapter
    }

    private fun DaoActionSpec.Query.getSQLQuery(
        functionSpec: DaoFunctionSpec
    ): SQLDaoQuery {
        val queryParts = value.split(" ")
        val parametersMap = functionSpec.parameters.associateBy(DaoParameterSpec::name)
        val sortedParameters = ArrayList<DaoParameterSpec>()

        val query = buildSQLQuery {
            queryParts.forEach { part ->
                if (part.startsWith(SQLSyntax.Sign.VALUE_PREFIX)) {
                    val parameterName = part.replace(SQLSyntax.Sign.VALUE_PREFIX, "")
                        .replace(SQLSyntax.Sign.STATEMENT_SEPARATOR, "")
                        .replace(SQLSyntax.Sign.FUNCTION_OPEN_PARENTHESES, "")
                        .replace(SQLSyntax.Sign.FUNCTION_CLOSE_PARENTHESES, "")
                        .trim()

                    val parameter = parametersMap[parameterName]
                    when (parameter?.typeSpec?.dataType) {
                        is DataTypeSpec.DataType.Class -> {
                            VALUE
                        }

                        is DataTypeSpec.DataType.Collection -> {
                            append("\$${parameterName}Indexes")
                        }

                        is DataTypeSpec.DataType.Entity,
                        is DataTypeSpec.DataType.Compound,
                        is DataTypeSpec.DataType.Stream -> error("not supported")

                        null -> {
                            logger.throwException(
                                "Can't find parameter spec for $parameterName among $parametersMap",
                                functionSpec.declaration
                            )
                        }
                    }

                    sortedParameters.add(parameter)
                } else {
                    append(part)
                }
            }
        }

        return SQLDaoQuery(query, sortedParameters)
    }

    private fun ColumnInfo.TypeAffinity.getBindFunction(): String = when (this) {
        ColumnInfo.TypeAffinity.INTEGER -> SqlPreparedStatement::bindLong.name
        ColumnInfo.TypeAffinity.NUMERIC -> SqlPreparedStatement::bindString.name
        ColumnInfo.TypeAffinity.REAL -> SqlPreparedStatement::bindDouble.name
        ColumnInfo.TypeAffinity.TEXT -> SqlPreparedStatement::bindString.name
        ColumnInfo.TypeAffinity.NONE -> SqlPreparedStatement::bindBytes.name
        ColumnInfo.TypeAffinity.UNDEFINED -> error("Can't find bind function for this type $this")
    }

    data class Result(
        val className: ClassName,
        val adapters: Set<ColumnAdapterReference>,
        val mappers: Set<MapperReference>
    )
}
