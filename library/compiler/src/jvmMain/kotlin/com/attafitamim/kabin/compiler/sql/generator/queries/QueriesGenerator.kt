package com.attafitamim.kabin.compiler.sql.generator.queries

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import com.attafitamim.kabin.annotations.ColumnInfo
import com.attafitamim.kabin.compiler.sql.generator.references.ColumnAdapterReference
import com.attafitamim.kabin.compiler.sql.generator.references.FunctionReference
import com.attafitamim.kabin.compiler.sql.generator.references.MapperReference
import com.attafitamim.kabin.compiler.sql.syntax.SQLQuery
import com.attafitamim.kabin.compiler.sql.utils.poet.DRIVER_NAME
import com.attafitamim.kabin.compiler.sql.utils.poet.SYMBOL_ACCESS_SIGN
import com.attafitamim.kabin.compiler.sql.utils.poet.asSpecs
import com.attafitamim.kabin.compiler.sql.utils.poet.buildSpec
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.getAdapterReference
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.getColumnAccessChain
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.supportedBinders
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.toParameterAccess
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.toReference
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.toReferences
import com.attafitamim.kabin.compiler.sql.utils.poet.references.asPropertyName
import com.attafitamim.kabin.compiler.sql.utils.poet.references.getPropertyName
import com.attafitamim.kabin.compiler.sql.utils.poet.simpleNameString
import com.attafitamim.kabin.compiler.sql.utils.poet.sqldelight.EXECUTE_FUNCTION
import com.attafitamim.kabin.compiler.sql.utils.poet.sqldelight.EXECUTE_QUERY_FUNCTION
import com.attafitamim.kabin.compiler.sql.utils.poet.sqldelight.addDriverQueryCode
import com.attafitamim.kabin.compiler.sql.utils.poet.toSimpleTypeName
import com.attafitamim.kabin.compiler.sql.utils.poet.writeType
import com.attafitamim.kabin.compiler.sql.utils.spec.findEntityDataType
import com.attafitamim.kabin.compiler.sql.utils.spec.getEntityDataType
import com.attafitamim.kabin.compiler.sql.utils.spec.getMainEntityAccess
import com.attafitamim.kabin.compiler.sql.utils.spec.getNestedDataType
import com.attafitamim.kabin.compiler.sql.utils.spec.getQueryFunctionName
import com.attafitamim.kabin.compiler.sql.utils.spec.toSortedSet
import com.attafitamim.kabin.compiler.sql.utils.sql.dao.getParameterReferences
import com.attafitamim.kabin.compiler.sql.utils.sql.dao.getSQLQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.dao.getSelectSQLQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.getFlatColumns
import com.attafitamim.kabin.compiler.sql.utils.sql.sqlType
import com.attafitamim.kabin.core.dao.KabinSuspendingTransactor
import com.attafitamim.kabin.core.table.KabinMapper
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.column.ColumnTypeSpec
import com.attafitamim.kabin.specs.dao.DaoActionSpec
import com.attafitamim.kabin.specs.dao.DaoFunctionSpec
import com.attafitamim.kabin.specs.dao.DaoParameterSpec
import com.attafitamim.kabin.specs.dao.DaoSpec
import com.attafitamim.kabin.specs.dao.DataTypeSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.attafitamim.kabin.specs.relation.compound.CompoundPropertySpec
import com.attafitamim.kabin.specs.relation.compound.CompoundRelationSpec
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSType
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
        val className = daoSpec.getQueryFunctionName(options)
        val superClassName = KabinSuspendingTransactor::class.asClassName()

        val classBuilder = TypeSpec.classBuilder(className)
            .superclass(superClassName)
            .addSuperclassConstructorParameter(DRIVER_NAME)

        val adapters = HashSet<ColumnAdapterReference>()
        val mappers = HashSet<MapperReference>()

        val addedFunctions = HashSet<FunctionReference>()
        daoSpec.functionSpecs.forEach { functionSpec ->
            val actionSpec = functionSpec.actionSpec
            if (actionSpec != null) {
                val returnTypeSpec = functionSpec.returnTypeSpec
                if (returnTypeSpec == null) {
                    val requiredAdapters = classBuilder.addVoidQueryFunction(
                        functionSpec,
                        actionSpec,
                        addedFunctions
                    )

                    adapters.addAll(requiredAdapters)
                } else {
                    val result = classBuilder.addResultQuery(
                        className,
                        functionSpec,
                        actionSpec,
                        returnTypeSpec,
                        functionSpec.declaration.simpleNameString,
                        addedFunctions
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
        parentName: String,
        addedFunctions: MutableSet<FunctionReference>
    ): Result {
        val dataReturnType = returnTypeSpec.getNestedDataType()
        val result = when (val dataType = dataReturnType.dataType as DataTypeSpec.DataType.Data) {
            is DataTypeSpec.DataType.Class -> {
                addSimpleResultQuery(
                    queriesClassName,
                    functionSpec,
                    parentName,
                    returnTypeSpec,
                    addedFunctions
                )
            }

            is DataTypeSpec.DataType.Entity -> {
                val query = when (actionSpec) {
                    is DaoActionSpec.QueryAction -> actionSpec.getSQLQuery(functionSpec, logger)
                    is DaoActionSpec.EntityAction -> actionSpec.getSQLQuery(dataType.entitySpec)
                }

                addEntityResultQuery(
                    queriesClassName,
                    dataReturnType,
                    query,
                    addedFunctions
                )
            }

            is DataTypeSpec.DataType.Compound -> {
                val mainEntity = dataType.compoundSpec.mainProperty.dataTypeSpec
                    .getEntityDataType()
                    .entitySpec

                val query = when (actionSpec) {
                    is DaoActionSpec.QueryAction -> actionSpec.getSQLQuery(functionSpec, logger)
                    is DaoActionSpec.EntityAction -> actionSpec.getSQLQuery(mainEntity)
                }

                addEntityResultQuery(
                    queriesClassName,
                    dataReturnType,
                    query,
                    addedFunctions
                )
            }
        }

        return result
    }

    private fun TypeSpec.Builder.addEntityResultQuery(
        queriesClassName: ClassName,
        dataTypeSpec: DataTypeSpec,
        query: SQLQuery,
        addedFunctions: MutableSet<FunctionReference>
    ): Result = when (val dataType = dataTypeSpec.dataType) {
        is DataTypeSpec.DataType.Entity -> {
            addEntityQuery(
                queriesClassName,
                query,
                dataType.entitySpec,
                addedFunctions
            )
        }

        is DataTypeSpec.DataType.Compound -> {
            val adapters = LinkedHashSet<ColumnAdapterReference>()
            val mappers = LinkedHashSet<MapperReference>()

            dataType.compoundSpec.relations.forEach { compoundRelationSpec ->
                val junctionSpec = compoundRelationSpec.relation.junctionSpec
                if (junctionSpec != null) {
                    val junctionColumn = junctionSpec.entitySpec
                        .getColumnAccessChain(junctionSpec.parentColumn)
                        .last()

                    val junctionQuery = getSelectSQLQuery(junctionSpec.entitySpec, junctionColumn)
                    val relationResult = addEntityQuery(
                        queriesClassName,
                        junctionQuery,
                        junctionSpec.entitySpec,
                        addedFunctions
                    )

                    adapters.addAll(relationResult.adapters)
                    mappers.addAll(relationResult.mappers)
                }

                val parentEntity = compoundRelationSpec.property.dataTypeSpec
                    .getEntityDataType()
                    .entitySpec

                val entityColumn = parentEntity
                    .getColumnAccessChain(compoundRelationSpec.relation.entityColumn)
                    .last()

                val newQuery = getSelectSQLQuery(parentEntity, entityColumn)
                val relationResult = addEntityResultQuery(
                    queriesClassName,
                    compoundRelationSpec.property.dataTypeSpec,
                    newQuery,
                    addedFunctions
                )

                adapters.addAll(relationResult.adapters)
                mappers.addAll(relationResult.mappers)
            }

            val result = addEntityResultQuery(
                queriesClassName,
                dataType.compoundSpec.mainProperty.dataTypeSpec,
                query,
                addedFunctions
            )

            adapters.addAll(result.adapters)
            mappers.addAll(result.mappers)

            Result(
                result.className,
                adapters,
                mappers
            )
        }

        is DataTypeSpec.DataType.Collection -> {
            val nestedTypeSpec = dataType.nestedTypeSpec.getNestedDataType()
            addEntityResultQuery(
                queriesClassName,
                nestedTypeSpec,
                query,
                addedFunctions
            )
        }

        is DataTypeSpec.DataType.Class,
        is DataTypeSpec.DataType.Stream -> error("not supported here")
    }

    private fun TypeSpec.Builder.addEntityQuery(
        queriesClassName: ClassName,
        query: SQLQuery,
        entitySpec: EntitySpec,
        addedFunctions: MutableSet<FunctionReference>
    ): Result {
        val adapters = HashSet<ColumnAdapterReference>()
        val mappers = HashSet<MapperReference>()

        val name = entitySpec.getQueryFunctionName(query)
        val queryClassName = ClassName(
            queriesClassName.packageName,
            queriesClassName.simpleName,
            name
        )

        val parameters = query.getParameterReferences()
        val reference = FunctionReference(name, parameters)
        if (addedFunctions.contains(reference)) {
            return Result(queryClassName, emptySet(), emptySet())
        }

        addedFunctions.add(reference)

        val returnType = entitySpec.declaration.toClassName()
        val superClass = Query::class.asClassName()
            .parameterizedBy(returnType)

        val constructorBuilder = FunSpec.constructorBuilder()

        val queryBuilder = TypeSpec.anonymousClassBuilder()
            .superclass(superClass)

        val functionBuilder = FunSpec.builder(reference.name)
            .returns(superClass)

        parameters.toSortedSet().forEach { parameterReference ->
            val parameterSpec = ParameterSpec.builder(
                parameterReference.name,
                parameterReference.type
            ).build()

            constructorBuilder.addParameter(parameterSpec)
            functionBuilder.addParameter(parameterSpec)

            val propertySpec = PropertySpec.builder(
                parameterReference.name,
                parameterReference.type,
                KModifier.PRIVATE
            ).initializer(parameterReference.name).build()
            queryBuilder.addProperty(propertySpec)
        }

        val queriedKeys = LinkedHashSet<String>()
        queriedKeys.add(entitySpec.tableName)
        queriedKeys.addAll(query.queriedKeys)

        val addListenerFunction = Query<*>::addListener.buildSpec()
            .addModifiers(KModifier.OVERRIDE)
            .addListenerLogic(queriedKeys, SqlDriver::addListener.name)
            .build()

        val removeListenerFunction = Query<*>::removeListener.buildSpec()
            .addModifiers(KModifier.OVERRIDE)
            .addListenerLogic(queriedKeys, SqlDriver::removeListener.name)
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

        executeFunctionBuilder.addDriverQueryCode(query) {
            val requiredAdapters = addQueryBinding(query)
            adapters.addAll(requiredAdapters)
        }

        queryBuilder
            .addFunction(addListenerFunction)
            .addFunction(removeListenerFunction)
            .addFunction(executeFunctionBuilder.build())

        functionBuilder.addStatement("return·%L", queryBuilder.build())
        addFunction(functionBuilder.build())

        return Result(
            queryClassName,
            adapters,
            mappers
        )
    }

    private fun TypeSpec.Builder.addVoidQueryFunction(
        daoFunctionSpec: DaoFunctionSpec,
        actionSpec: DaoActionSpec,
        addedFunctions: MutableSet<FunctionReference>
    ): Set<ColumnAdapterReference> {
        val reference = daoFunctionSpec.toReference()
        if (addedFunctions.contains(reference)) {
            return emptySet()
        }

        addedFunctions.add(reference)

        val adapters = HashSet<ColumnAdapterReference>()
        val builder = FunSpec.builder(reference.name)
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

            is DaoActionSpec.QueryAction -> {
                val query = actionSpec.getSQLQuery(daoFunctionSpec, logger)

                builder.addDriverQueryCode(
                    query,
                    EXECUTE_FUNCTION
                ) {
                    val requiredAdapters = addQueryBinding(query)
                    adapters.addAll(requiredAdapters)
                }
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
        name: String? = null,
        relationSpec: CompoundRelationSpec? = null,
        mainPropertySpec: CompoundPropertySpec? = null,
        parent: String? = null
    ): Set<ColumnAdapterReference> {
        val adapters = HashSet<ColumnAdapterReference>()
        val parameterName = name ?: daoParameterSpec.name

        if (dataTypeSpec.isNullable) {
            beginControlFlow("$parameterName?.let·{")
        }

        when (val dataType = dataTypeSpec.dataType) {
            is DataTypeSpec.DataType.Class,
            is DataTypeSpec.DataType.Stream -> {
                val annotationName = actionSpec.javaClass.simpleName
                logger.throwException(
                    "Only entities are allowed as queryParameters for $annotationName functions",
                    daoFunctionSpec.declaration
                )
            }

            is DataTypeSpec.DataType.Entity -> {
                val query = actionSpec.getSQLQuery(dataType.entitySpec)
                addDriverQueryCode(
                    query,
                    EXECUTE_FUNCTION,
                ) {
                    val requiredAdapters = addQueryEntityBinding(
                        query.columns,
                        parameterName
                    )

                    adapters.addAll(requiredAdapters)
                }
            }

            is DataTypeSpec.DataType.Compound -> {
                dataType.compoundSpec.mainProperty.let { mainProperty ->
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

                dataType.compoundSpec.relations.forEach { compoundRelationSpec ->
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
                        propertyAccess,
                        compoundRelationSpec,
                        dataType.compoundSpec.mainProperty,
                        parameterName
                    )

                    adapters.addAll(requiredAdapters)
                }
            }

            is DataTypeSpec.DataType.Collection -> {
                val childName = buildString {
                    append(parameterName.asPropertyName(), "Child")
                }

                beginControlFlow("$parameterName.forEach·{·$childName·->")

                val requiredAdapters = addParameterEntityActionQuery(
                    daoFunctionSpec,
                    daoParameterSpec,
                    actionSpec,
                    dataType.nestedTypeSpec,
                    childName
                )

                val junction = relationSpec?.relation?.junctionSpec
                if (junction != null && mainPropertySpec != null) {
                    val junctionEntity = junction.entitySpec
                    val parentEntityAccess = mainPropertySpec
                        .getMainEntityAccess()

                    val parentEntity = parentEntityAccess
                        .last()
                        .dataTypeSpec
                        .getEntityDataType()
                        .entitySpec

                    val childEntityParent = dataTypeSpec.getNestedDataType()
                    val childEntityParentType = childEntityParent.dataType

                    val childEntity: EntitySpec
                    val childEntityAccess: String
                    when (childEntityParentType) {
                        is DataTypeSpec.DataType.Entity -> {
                            childEntity = childEntityParentType.entitySpec
                            childEntityAccess = ""
                        }

                        is DataTypeSpec.DataType.Compound -> {
                            val mainEntityAccess = childEntityParentType
                                .compoundSpec
                                .mainProperty
                                .getMainEntityAccess()

                            childEntity = mainEntityAccess
                                .last()
                                .dataTypeSpec
                                .getEntityDataType()
                                .entitySpec

                            childEntityAccess = mainEntityAccess.joinToString(SYMBOL_ACCESS_SIGN) { compoundPropertySpec ->
                                compoundPropertySpec.declaration.simpleNameString
                            }
                        }

                        else -> {
                            error("not supported here")
                        }
                    }

                    val parentColumnAccess = parentEntity
                        .getColumnAccessChain(relationSpec.relation.parentColumn)
                        .toParameterAccess()

                    val entityColumnAccess = childEntity
                        .getColumnAccessChain(relationSpec.relation.entityColumn)
                        .toParameterAccess()

                    val junctionName = "${childName}Junction"
                    val parentsAccess = parentEntityAccess.joinToString(SYMBOL_ACCESS_SIGN) { compoundPropertySpec ->
                        compoundPropertySpec.declaration.simpleNameString
                    }

                    val fullParentAccess = "$parent.$parentsAccess.$parentColumnAccess"
                    val fullEntityAccess = if (childEntityAccess.isBlank()) {
                        "$childName.$entityColumnAccess"
                    } else {
                        "$childName.$childEntityAccess.$entityColumnAccess"
                    }

                    val junctionEntityColumnName = junctionEntity
                        .getColumnAccessChain(junction.entityColumn)
                        .last()
                        .declaration
                        .simpleNameString

                    val junctionParentColumnName = junctionEntity
                        .getColumnAccessChain(junction.parentColumn)
                        .last()
                        .declaration
                        .simpleNameString

                    addStatement(
                        "val $junctionName = %T($junctionParentColumnName = $fullParentAccess, $junctionEntityColumnName = $fullEntityAccess)",
                        junctionEntity.declaration.toClassName()
                    )

                    val query = actionSpec.getSQLQuery(junctionEntity)
                    addDriverQueryCode(
                        query,
                        EXECUTE_FUNCTION
                    ) {
                        val junctionAdapters = addQueryEntityBinding(
                            query.columns,
                            junctionName
                        )

                        adapters.addAll(junctionAdapters)
                    }
                }

                endControlFlow()
                adapters.addAll(requiredAdapters)
            }
        }

        if (dataTypeSpec.isNullable) {
            endControlFlow()
        }

        return adapters
    }

    private fun TypeSpec.Builder.addSimpleResultQuery(
        queriesClassName: ClassName,
        functionSpec: DaoFunctionSpec,
        name: String,
        returnTypeSpec: DataTypeSpec,
        addedFunctions: MutableSet<FunctionReference>
    ): Result {
        val queryClassName = ClassName(
            queriesClassName.packageName,
            queriesClassName.simpleName,
            name
        )

        val typeName = TypeVariableName.invoke("R")

        val reference = FunctionReference(name, functionSpec.parameters.toReferences())
        if (addedFunctions.contains(reference)) {
            return Result(queriesClassName, emptySet(), emptySet())
        }

        addedFunctions.add(reference)

        val adapters = LinkedHashSet<ColumnAdapterReference>()
        val mappers = LinkedHashSet<MapperReference>()

        val returnType = returnTypeSpec.getNestedDataType().type.toTypeName()
            .copy(nullable = false)

        val superClass = Query::class.asClassName()
            .parameterizedBy(returnType)

        val constructorBuilder = FunSpec.constructorBuilder()

        val queryBuilder = TypeSpec.anonymousClassBuilder()
            .superclass(superClass)

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

        val queriedKeys = LinkedHashSet<String>()
        val returnEntity = returnTypeSpec.findEntityDataType()
        returnEntity?.entitySpec?.tableName?.let(queriedKeys::add)

        when (val actionSpec = functionSpec.actionSpec!!) {
            is DaoActionSpec.EntityAction -> {
                functionSpec.parameters.forEach { entityParameter ->
                    val dataTypeSpec = entityParameter.typeSpec.dataType as DataTypeSpec.DataType.Entity
                    val query = actionSpec.getSQLQuery(dataTypeSpec.entitySpec)

                    executeFunctionBuilder.addDriverQueryCode(
                        query,
                        EXECUTE_QUERY_FUNCTION
                    ) {
                        val bindingAdapters = addQueryEntityBinding(
                            query.columns,
                            entityParameter.name
                        )

                        adapters.addAll(bindingAdapters)
                    }

                    queriedKeys.addAll(query.queriedKeys)
                }
            }

            is DaoActionSpec.QueryAction -> {
                val query = actionSpec.getSQLQuery(functionSpec, logger)
                executeFunctionBuilder.addDriverQueryCode(query) {
                    val bindingAdapters = addQueryBinding(query)
                    adapters.addAll(bindingAdapters)
                }

                queriedKeys.addAll(query.queriedKeys)
            }
        }

        val addListenerFunction = Query<*>::addListener.buildSpec()
            .addModifiers(KModifier.OVERRIDE)
            .addListenerLogic(queriedKeys, SqlDriver::addListener.name)
            .build()

        val removeListenerFunction = Query<*>::removeListener.buildSpec()
            .addModifiers(KModifier.OVERRIDE)
            .addListenerLogic(queriedKeys, SqlDriver::removeListener.name)
            .build()

        queryBuilder
            .addFunction(addListenerFunction)
            .addFunction(removeListenerFunction)
            .addFunction(executeFunctionBuilder.build())

        val parameterNames = ArrayList<String>(functionSpec.parameters.size)
        val parametersSpec = functionSpec.parameters.map { daoParameterSpec ->
            parameterNames.add(daoParameterSpec.name)
            daoParameterSpec.declaration.buildSpec().build()
        }

        val queryReturnType = Query::class.asClassName()
            .parameterizedBy(returnType)

        val builder = FunSpec.builder(reference.name)
            .addParameters(parametersSpec)
            .addStatement("return·%L", queryBuilder.build())
            .returns(queryReturnType)

        addFunction(builder.build())

        return Result(
            queryClassName,
            adapters,
            mappers
        )
    }

    private fun FunSpec.Builder.addListenerLogic(
        keys: Set<String>,
        listenerMethod: String
    ): FunSpec.Builder = apply {
        if (keys.isEmpty()) {
            addStatement("// No queryKeys detected for listening")
            addStatement("// If you are using @RawQuery, make sure to use observableEntities parameter")
            return@apply
        }

        val driverName = DRIVER_NAME
        addStatement("$driverName.$listenerMethod(")
        keys.forEach { key ->
            addStatement("%S,", key)
        }

        addStatement("listener·=·listener")
        addStatement(")")
    }

    private fun CodeBlock.Builder.addQueryEntityBinding(
        columns: Collection<ColumnSpec>,
        parent: String? = null
    ): Set<ColumnAdapterReference> {
        if (columns.isEmpty()) {
            return emptySet()
        }

        beginControlFlow("")
        val adapters = addQueryEntityColumnsBinding(
            columns,
            parent
        )

        endControlFlow()
        return adapters
    }

    private fun CodeBlock.Builder.addQueryBinding(
        query: SQLQuery
    ): Set<ColumnAdapterReference> = when (query) {
        is SQLQuery.Columns -> addQueryEntityBinding(query.columns)
        is SQLQuery.Parameters -> addQueryParametersBinding(query.queryParameters)
        is SQLQuery.Raw -> emptySet()
    }

    private fun CodeBlock.Builder.addQueryEntityColumnsBinding(
        columns: Collection<ColumnSpec>,
        parent: String? = null,
        initialIndex: Int = 0,
        isParentNullable: Boolean = false
    ): Set<ColumnAdapterReference> {
        val adapters = HashSet<ColumnAdapterReference>()

        var currentIndex = initialIndex
        columns.forEach { columnSpec ->
            val isNullable = isParentNullable || columnSpec.typeSpec.isNullable
            val propertyName = columnSpec.declaration.simpleName.asString()
            val propertyAccess = if (parent.isNullOrBlank()) {
                propertyName
            } else {
                buildString {
                    append(parent)

                    if (isParentNullable) {
                        append("?")
                    }

                    append(
                        SYMBOL_ACCESS_SIGN,
                        propertyName
                    )
                }
            }

            when (val dataType = columnSpec.typeSpec.dataType) {
                is ColumnTypeSpec.DataType.Class -> {
                    val adapter = addQueryParameterBinding(
                        isNullable,
                        propertyAccess,
                        currentIndex.toString(),
                        columnSpec.typeAffinity,
                        columnSpec.typeSpec.type
                    )

                    if (adapter != null) {
                        adapters.add(adapter)
                    }

                    currentIndex++
                }

                is ColumnTypeSpec.DataType.Embedded -> {
                    val requiredAdapters = addQueryEntityColumnsBinding(
                        dataType.columns,
                        propertyAccess,
                        currentIndex,
                        isNullable
                    )

                    adapters.addAll(requiredAdapters)
                    currentIndex += getFlatColumns(dataType.columns).size
                }
            }
        }

        return adapters
    }

    private fun CodeBlock.Builder.addQueryParametersBinding(
        parameterSpecs: Collection<SQLQuery.Parameters.QueryParameter>,
    ): Set<ColumnAdapterReference> {
        if (parameterSpecs.isEmpty()) {
            return emptySet()
        }

        val adapters = HashSet<ColumnAdapterReference>()
        beginControlFlow("")

        var previousSimpleParametersCount = 0
        val previousDynamicParameters = ArrayList<SQLQuery.Parameters.QueryParameter>()
        parameterSpecs.forEach { queryParameter ->
            val dynamicSizes = previousDynamicParameters.joinToString(" + ") {
                "${it.spec.name}Size"
            }

            val indexExpression = if (dynamicSizes.isBlank()) {
                previousSimpleParametersCount.toString()
            } else if (previousSimpleParametersCount == 0) {
                dynamicSizes
            } else {
                "$dynamicSizes + $previousSimpleParametersCount"
            }


            when (queryParameter.spec.typeSpec.dataType) {
                is DataTypeSpec.DataType.Entity,
                is DataTypeSpec.DataType.Stream,
                is DataTypeSpec.DataType.Compound -> error("not supported here")
                is DataTypeSpec.DataType.Collection -> {
                    if (queryParameter.previousKeyword == "IN") {
                        previousDynamicParameters.add(queryParameter)
                        val requiredAdapters = addQueryParameterSpecBinding(
                            queryParameter.spec.name,
                            queryParameter.spec.typeSpec,
                            indexExpression
                        )

                        adapters.addAll(requiredAdapters)
                    }
                }

                is DataTypeSpec.DataType.Class -> {
                    previousSimpleParametersCount++

                    val requiredAdapters = addQueryParameterSpecBinding(
                        queryParameter.spec.name,
                        queryParameter.spec.typeSpec,
                        indexExpression
                    )

                    adapters.addAll(requiredAdapters)
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

        when (val dataType = dataTypeSpec.dataType) {
            is DataTypeSpec.DataType.Entity,
            is DataTypeSpec.DataType.Compound,
            is DataTypeSpec.DataType.Stream -> logger.throwException(
                "Only primitive values are allowed as query queryParameters"
            )

            is DataTypeSpec.DataType.Class -> {
                val adapter = addQueryParameterBinding(
                    dataTypeSpec.isNullable,
                    actualParameterName,
                    index,
                    dataTypeSpec.type.sqlType,
                    dataTypeSpec.type
                )

                if (adapter != null) {
                    adapters.add(adapter)
                }
            }

            is DataTypeSpec.DataType.Collection -> {
                val childName = buildString {
                    append(actualParameterName, "Child")
                }

                if (dataTypeSpec.isNullable) {
                    beginControlFlow("$actualParameterName?.forEachIndexed { index, $childName ->")
                } else {
                    beginControlFlow("$actualParameterName.forEachIndexed { index, $childName ->")
                }

                val indexExpression = if (index == "0") {
                    "index"
                } else {
                    "index + $index"
                }

                val requiredAdapters = addQueryParameterSpecBinding(
                    actualParameterName,
                    dataType.nestedTypeSpec,
                    indexExpression,
                    childName
                )

                endControlFlow()
                adapters.addAll(requiredAdapters)
            }
        }

        return adapters
    }

    private fun CodeBlock.Builder.addQueryParameterBinding(
        isNullable: Boolean,
        parameter: String,
        index: String,
        typeAffinity: ColumnInfo.TypeAffinity?,
        type: KSType
    ): ColumnAdapterReference? {
        val declarationAffinity = type.sqlType
        val actualTypeAffinity = typeAffinity ?: declarationAffinity

        val adapter = type.getAdapterReference(typeAffinity)
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
            parameter to supportedBinders.getValue(type.toSimpleTypeName())
        }

        addStatement("$bindFunction($index,·$actualParameter)")
        return adapter
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
