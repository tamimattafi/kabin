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
import com.attafitamim.kabin.compiler.sql.utils.poet.toCamelCase
import com.attafitamim.kabin.compiler.sql.utils.poet.typeInitializer
import com.attafitamim.kabin.compiler.sql.utils.poet.writeType
import com.attafitamim.kabin.compiler.sql.utils.spec.getQueryClassName
import com.attafitamim.kabin.compiler.sql.utils.sql.dao.getSQLQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.sqlType
import com.attafitamim.kabin.core.table.KabinMapper
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.utils.resolveClassDeclaration
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.dao.DaoActionSpec
import com.attafitamim.kabin.specs.dao.DaoFunctionSpec
import com.attafitamim.kabin.specs.dao.DaoParameterSpec
import com.attafitamim.kabin.specs.dao.DaoSpec
import com.attafitamim.kabin.specs.dao.DataTypeSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
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
                    val result = classBuilder.addResultQueryClass(
                        className,
                        functionSpec,
                        actionSpec,
                        returnTypeSpec
                    )

                    adapters.addAll(result.adapters)
                    mappers.addAll(result.mappers)

                    val requiredAdapters = classBuilder.addResultQueryFunction(
                        result.className,
                        functionSpec,
                        returnTypeSpec
                    )

                    adapters.addAll(requiredAdapters)
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
                val query = actionSpec.getSQLQuery()
                val codeBlockBuilder = CodeBlock.builder()
                    .addStatement("driver.execute(")
                    .addStatement("${query.hashCode()},")
                    .addStatement("%S,", query.value)
                    .addStatement(query.parameters.size.toString())
                    .addStatement(")")

                val parameterAdapters = codeBlockBuilder.addQueryParametersBinding(
                    daoFunctionSpec.parameters,
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
                    query.parameters.size
                ) {
                    val requiredAdapters = addQueryEntityBinding(
                        parameterName,
                        dataType.spec,
                        query.parameters
                    )

                    adapters.addAll(requiredAdapters)
                }

                addStatement("""
                notifyQueries(${query.hashCode()}) { emit ->
                    emit(%S)
                }
                """.trimIndent(), dataType.spec.tableName)
            }

            is DataTypeSpec.DataType.Collection -> {
                val childName = buildString {
                    append(parameterName, "Child")
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
        daoFunctionSpec: DaoFunctionSpec,
        actionSpec: DaoActionSpec,
        returnTypeSpec: DataTypeSpec
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
            daoFunctionSpec.declaration.simpleNameString.toCamelCase()
        )

        val queryBuilder = TypeSpec.classBuilder(queryClassName)
            .superclass(superClass).addModifiers(KModifier.INNER)

        daoFunctionSpec.parameters.forEach { parameterSpec ->
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
            .addListenerLogic(daoFunctionSpec.returnTypeSpec, SqlDriver::addListener.name)
            .build()

        val removeListenerFunction = Query<*>::removeListener.buildSpec()
            .addModifiers(KModifier.OVERRIDE)
            .addListenerLogic(daoFunctionSpec.returnTypeSpec, SqlDriver::removeListener.name)
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
                daoFunctionSpec.parameters.forEach { entityParameter ->
                    val dataTypeSpec = entityParameter.typeSpec.dataType as DataTypeSpec.DataType.Entity
                    val query = actionSpec.getSQLQuery(dataTypeSpec.spec)

                    executeFunctionBuilder.addDriverQueryCode(
                        query.hashCode(),
                        query.value,
                        query.parameters.size
                    ) {
                        val bindingAdapters = addQueryEntityBinding(
                            entityParameter.name,
                            dataTypeSpec.spec,
                            query.parameters
                        )

                        adapters.addAll(bindingAdapters)
                    }
                }
            }

            is DaoActionSpec.Query -> {
                val query = actionSpec.getSQLQuery()
                executeFunctionBuilder.addDriverQueryCode(
                    query.hashCode(),
                    query.value,
                    query.parameters.size
                ) {
                    val bindingAdapters = addQueryParametersBinding(
                        daoFunctionSpec.parameters,
                        query.parameters
                    )

                    adapters.addAll(bindingAdapters)
                }
            }

            is DaoActionSpec.RawQuery -> {
                val rawQuery = daoFunctionSpec.parameters.first().name
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

    private fun TypeSpec.Builder.addResultQueryFunction(
        queryClassName: ClassName,
        daoFunctionSpec: DaoFunctionSpec,
        returnTypeSpec: DataTypeSpec
    ): Set<ColumnAdapterReference> {
        val adapters = HashSet<ColumnAdapterReference>()

        val returnType = returnTypeSpec.getDataReturnType().declaration.toClassName()
        val builder = FunSpec.builder(daoFunctionSpec.declaration.simpleName.asString())
            .addParameters(daoFunctionSpec.declaration.parameters.asSpecs())

        val queryReturnType = Query::class.asClassName()
            .parameterizedBy(returnType)

        builder.returns(queryReturnType)

        val queryReturnBlock = CodeBlock.builder()
        val typeInitializer = typeInitializer(
            daoFunctionSpec.parameters.map(DaoParameterSpec::name),
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
            is DataTypeSpec.DataType.Class -> return@apply

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
        name: String,
        entitySpec: EntitySpec,
        parameters: Collection<String>
    ): Set<ColumnAdapterReference> {
        if (parameters.isEmpty()) {
            return emptySet()
        }

        val adapters = HashSet<ColumnAdapterReference>()
        beginControlFlow("")

        val columnsMap = entitySpec.columns.associateBy(ColumnSpec::name)
        parameters.forEachIndexed { index, parameter ->
            val column = columnsMap.getValue(parameter)
            val propertyName = column.declaration.simpleName.asString()
            val propertyAccess = buildString {
                append(
                    name,
                    SYMBOL_ACCESS_SIGN,
                    propertyName
                )
            }

            val adapter = addQueryParameterBinding(
                column.isNullable,
                propertyAccess,
                index,
                column.typeAffinity,
                column.declaration.type.resolveClassDeclaration()
            )

            if (adapter != null) {
                adapters.add(adapter)
            }
        }

        endControlFlow()
        return adapters
    }

    private fun CodeBlock.Builder.addQueryParametersBinding(
        parameterSpecs: List<DaoParameterSpec>,
        parameters: Collection<String>
    ): Set<ColumnAdapterReference> {
        if (parameters.isEmpty()) {
            return emptySet()
        }

        val adapters = HashSet<ColumnAdapterReference>()
        beginControlFlow("")

        val parametersMap = parameterSpecs.associateBy(DaoParameterSpec::name)
        parameters.forEachIndexed { index, parameter ->
            val parameterSpec = parametersMap.getValue(parameter)
            val typeDeclaration = parameterSpec.declaration
                .type
                .resolveClassDeclaration()

            val adapter = addQueryParameterBinding(
                parameterSpec.typeSpec.isNullable,
                parameterSpec.name,
                index,
                typeDeclaration.sqlType,
                typeDeclaration
            )

            if (adapter != null) {
                adapters.add(adapter)
            }
        }

        endControlFlow()
        return adapters
    }

    private fun CodeBlock.Builder.addQueryParameterBinding(
        isNullable: Boolean,
        parameter: String,
        index: Int,
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

    private fun DataTypeSpec.getDataReturnType(): DataTypeSpec {
        var currentType = this
        var currentTypeDataType = currentType.dataType

        while (currentTypeDataType is DataTypeSpec.DataType.Wrapper) {
            currentType = currentTypeDataType.wrappedDeclaration
            currentTypeDataType = currentType.dataType
        }

        return currentType
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
