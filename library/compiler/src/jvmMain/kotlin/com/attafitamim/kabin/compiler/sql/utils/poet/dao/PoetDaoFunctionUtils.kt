package com.attafitamim.kabin.compiler.sql.utils.poet.dao

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.compiler.sql.utils.poet.SYMBOL_ACCESS_SIGN
import com.attafitamim.kabin.compiler.sql.utils.poet.asSpecs
import com.attafitamim.kabin.compiler.sql.utils.poet.buildSpec
import com.attafitamim.kabin.compiler.sql.utils.poet.qualifiedNameString
import com.attafitamim.kabin.compiler.sql.generator.references.ColumnAdapterReference
import com.attafitamim.kabin.compiler.sql.generator.references.MapperReference
import com.attafitamim.kabin.compiler.sql.utils.poet.DRIVER_NAME
import com.attafitamim.kabin.compiler.sql.utils.poet.entity.supportedAffinity
import com.attafitamim.kabin.compiler.sql.utils.poet.references.getPropertyName
import com.attafitamim.kabin.compiler.sql.utils.poet.simpleNameString
import com.attafitamim.kabin.compiler.sql.utils.poet.sqldelight.addDriverExecutionCode
import com.attafitamim.kabin.compiler.sql.utils.poet.sqldelight.addDriverQueryCode
import com.attafitamim.kabin.compiler.sql.utils.poet.toCamelCase
import com.attafitamim.kabin.compiler.sql.utils.sql.dao.getSQLQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.sqlType
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.utils.resolveClassDeclaration
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.core.TypeDeclaration
import com.attafitamim.kabin.specs.dao.DaoActionSpec
import com.attafitamim.kabin.specs.dao.DaoFunctionParameterSpec
import com.attafitamim.kabin.specs.dao.DaoFunctionSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName

val supportedBinders = mapOf(
    Long::class.qualifiedName to SqlPreparedStatement::bindLong.name,
    Double::class.qualifiedName to SqlPreparedStatement::bindDouble.name,
    String::class.qualifiedName to SqlPreparedStatement::bindString.name,
    ByteArray::class.qualifiedName to SqlPreparedStatement::bindBytes.name,
    Boolean::class.qualifiedName to SqlPreparedStatement::bindBoolean.name
)

fun TypeSpec.Builder.addQueryFunction(
    daoFunctionSpec: DaoFunctionSpec,
    options: KabinOptions
): Pair<Set<ColumnAdapterReference>, Set<MapperReference>> {
    val actionSpec = requireNotNull(daoFunctionSpec.actionSpec)

    val builder = FunSpec.builder(daoFunctionSpec.declaration.simpleName.asString())
        .addModifiers(KModifier.SUSPEND)
        .addParameters(daoFunctionSpec.declaration.parameters.asSpecs())

    val adapters = HashSet<ColumnAdapterReference>()
    val mappers = HashSet<MapperReference>()

    val returnType = daoFunctionSpec.returnType?.declaration?.toClassName()
    if (returnType == null || returnType == Unit::class.asTypeName()) {
        //builder.returns(Long::class)

        when (actionSpec) {
            is DaoActionSpec.EntityAction -> {
                daoFunctionSpec.parameters.forEach { entityParameter ->
                    val typeDeclaration = entityParameter.typeDeclaration as TypeDeclaration.Entity
                    val query = actionSpec.getSQLQuery(typeDeclaration.spec)

                    builder.addDriverExecutionCode(
                        query.hashCode(),
                        query.value,
                        query.parameters.size
                    ) {
                        val bindingAdapters = addQueryEntityBinding(
                            entityParameter,
                            typeDeclaration.spec,
                            query.parameters
                        )

                        adapters.addAll(bindingAdapters)
                    }

                    builder.addStatement("""
                        notifyQueries(${query.hashCode()}) { emit ->
                          emit(%S)
                        }
                    """.trimIndent(), typeDeclaration.spec.tableName)
                }
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

    } else {
        val queryReturnType = Query::class.asClassName()
            .parameterizedBy(returnType)

        builder.returns(queryReturnType)

        val constructorBuilder = FunSpec.constructorBuilder()
        val queryClassName = daoFunctionSpec.declaration.simpleNameString.toCamelCase()
        val queryBuilder = TypeSpec.classBuilder(queryClassName)
            .superclass(queryReturnType).addModifiers(KModifier.INNER)

        daoFunctionSpec.parameters.forEach { parameterSpec ->
            val parameterClassName = parameterSpec.typeDeclaration.declaration.toClassName()
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

        val mapperReference = MapperReference(returnType)
        val mapperName = mapperReference.getPropertyName(options)
        queryBuilder.primaryConstructor(constructorBuilder.build())
            .addSuperclassConstructorParameter("$mapperName::map")

        mappers.add(mapperReference)

        val typeName = TypeVariableName.invoke("R")
        val queryResultType = QueryResult::class.asClassName().parameterizedBy(typeName)
        val mapperParameterType = LambdaTypeName.get(
            SqlCursor::class.asTypeName(),
            returnType = queryResultType,
        )

        val addListenerFunction = Query<*>::addListener.buildSpec()
            .addModifiers(KModifier.OVERRIDE)
            .addListenerLogic(daoFunctionSpec.returnType, SqlDriver::addListener.name)
            .build()

        val removeListenerFunction = Query<*>::removeListener.buildSpec()
            .addModifiers(KModifier.OVERRIDE)
            .addListenerLogic(daoFunctionSpec.returnType, SqlDriver::removeListener.name)
            .build()

        val executeFunctionBuilder = FunSpec.builder("execute")
            .addTypeVariable(typeName)
            .returns(queryResultType)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("mapper", mapperParameterType)

        when (actionSpec) {
            is DaoActionSpec.EntityAction -> {
                daoFunctionSpec.parameters.forEach { entityParameter ->
                    val typeDeclaration = entityParameter.typeDeclaration as TypeDeclaration.Entity
                    val query = actionSpec.getSQLQuery(typeDeclaration.spec)

                    executeFunctionBuilder.addDriverQueryCode(
                        query.hashCode(),
                        query.value,
                        query.parameters.size
                    ) {
                        val bindingAdapters = addQueryEntityBinding(
                            entityParameter,
                            typeDeclaration.spec,
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

        val queryReturnBlock = CodeBlock.builder()
        queryReturnBlock.addStatement("return $queryClassName(${daoFunctionSpec.parameters.joinToString { it.name }})")
        builder.addCode(queryReturnBlock.build())
    }

    val funSpec = builder.build()
    addFunction(funSpec)
    return adapters to mappers
}

private fun FunSpec.Builder.addListenerLogic(
    returnType: TypeDeclaration?,
    listenerMethod: String
): FunSpec.Builder = apply {
    when (returnType) {
        null,
        is TypeDeclaration.Class -> return@apply

        is TypeDeclaration.Entity -> {
            val driverName = DRIVER_NAME
            addStatement(
                "$driverName.$listenerMethod(%S, listener = listener)",
                returnType.spec.tableName
            )
        }

        is TypeDeclaration.Flow -> {
            return addListenerLogic(returnType.elementDeclaration, listenerMethod)
        }
        is TypeDeclaration.List -> {
            return addListenerLogic(returnType.elementDeclaration, listenerMethod)
        }
    }
}

fun CodeBlock.Builder.addQueryEntityBinding(
    entityParameter: DaoFunctionParameterSpec,
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
                entityParameter.name,
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


fun CodeBlock.Builder.addQueryParametersBinding(
    parameterSpecs: List<DaoFunctionParameterSpec>,
    parameters: Collection<String>
): Set<ColumnAdapterReference> {
    if (parameters.isEmpty()) {
        return emptySet()
    }

    val adapters = HashSet<ColumnAdapterReference>()
    beginControlFlow("")

    val parametersMap = parameterSpecs.associateBy(DaoFunctionParameterSpec::name)
    parameters.forEachIndexed { index, parameter ->
        val parameterSpec = parametersMap.getValue(parameter)
        val typeDeclaration = parameterSpec.declaration
            .type
            .resolveClassDeclaration()

        val adapter = addQueryParameterBinding(
            parameterSpec.isNullable,
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

fun CodeBlock.Builder.addQueryParameterBinding(
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

fun ColumnInfo.TypeAffinity.getBindFunction(): String = when (this) {
    ColumnInfo.TypeAffinity.INTEGER -> SqlPreparedStatement::bindLong.name
    ColumnInfo.TypeAffinity.NUMERIC -> SqlPreparedStatement::bindString.name
    ColumnInfo.TypeAffinity.REAL -> SqlPreparedStatement::bindDouble.name
    ColumnInfo.TypeAffinity.TEXT -> SqlPreparedStatement::bindString.name
    ColumnInfo.TypeAffinity.NONE -> SqlPreparedStatement::bindBytes.name
    ColumnInfo.TypeAffinity.UNDEFINED -> error("Can't find bind function for this type $this")
}

fun KSDeclaration.needsConvert(
    typeAffinity: ColumnInfo.TypeAffinity?
): Boolean {
    val isSameAffinity = typeAffinity == null ||
            typeAffinity == ColumnInfo.TypeAffinity.UNDEFINED ||
            typeAffinity == sqlType

    return !isSameAffinity || !supportedBinders.containsKey(qualifiedNameString)
}

fun KSClassDeclaration.getAdapterReference(
    typeAffinity: ColumnInfo.TypeAffinity?
): ColumnAdapterReference? {
    if (!needsConvert(typeAffinity)) {
        return null
    }

    val actualAffinity = typeAffinity ?: sqlType
    val affinityType = supportedAffinity.getValue(actualAffinity).asClassName()
    return ColumnAdapterReference(
        affinityType,
        toClassName()
    )
}
