package com.attafitamim.kabin.compiler.sql.utils.poet.dao

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.SqlPreparedStatement
import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.Sign.VALUE_PREFIX
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.VALUE
import com.attafitamim.kabin.compiler.sql.utils.poet.SYMBOL_ACCESS_SIGN
import com.attafitamim.kabin.compiler.sql.utils.poet.asSpecs
import com.attafitamim.kabin.compiler.sql.utils.sql.buildSQLQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.column.sqlType
import com.attafitamim.kabin.compiler.sql.utils.sql.dao.getSQLQuery
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.core.TypeSpec
import com.attafitamim.kabin.specs.dao.DaoActionSpec
import com.attafitamim.kabin.specs.dao.DaoFunctionParameterSpec
import com.attafitamim.kabin.specs.dao.DaoFunctionSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.symbol.KSDeclaration
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName

fun DaoFunctionSpec.buildQueriesSpec(): FunSpec {
    val actionSpec = requireNotNull(actionSpec)

    val builder = FunSpec.builder(declaration.simpleName.asString())
        .addModifiers(KModifier.SUSPEND)
        .addParameters(declaration.parameters.asSpecs())

    val returnType = declaration.returnType?.toTypeName()
    if (returnType == null || returnType == Unit::class.asTypeName()) {
        //builder.returns(Long::class)
    } else {
        val queryReturnType = Query::class.asClassName()
            .parameterizedBy(returnType)

        builder.returns(queryReturnType)
    }

    when (actionSpec) {
        is DaoActionSpec.EntityAction -> {
            parameters.forEach { entityParameter ->
                val typeSpec = entityParameter.typeSpec as TypeSpec.Entity
                val query = actionSpec.getSQLQuery(typeSpec.spec)

                val codeBlock = CodeBlock.builder()
                    .addStatement("driver.execute(")
                    .addStatement("${query.hashCode()},")
                    .addStatement("%S,", query.value)
                    .addStatement(query.parameters.size.toString())
                    .addStatement(")")
                    .addQueryParametersBinding(entityParameter, typeSpec.spec, query.parameters)
                    .addStatement(".await()")
                    .build()

                builder.addCode(codeBlock)
            }
        }

        is DaoActionSpec.Query -> {
            val queryParts = actionSpec.value.split(" ")
            val newQuery = buildSQLQuery {
                queryParts.forEach { part ->
                    if (part.startsWith(VALUE_PREFIX)) {
                        VALUE
                    } else {
                        append(part)
                    }
                }
            }

            builder.addStatement("%S", newQuery)
        }

        is DaoActionSpec.RawQuery -> {
            val rawQuery = parameters.first().name
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

    return builder.build()
}

private fun CodeBlock.Builder.addQueryParametersBinding(
    entityParameter: DaoFunctionParameterSpec,
    entitySpec: EntitySpec,
    parameters: Collection<String>
): CodeBlock.Builder = apply {
    if (parameters.isEmpty()) {
        return@apply
    }

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

        addQueryParameterBinding(
            propertyAccess,
            index,
            column.sqlType
        )
    }

    endControlFlow()
}

private fun CodeBlock.Builder.addQueryParameterBinding(
    parameter: String,
    index: Int,
    typeAffinity: ColumnInfo.TypeAffinity,
    typeDeclaration: KSDeclaration
): CodeBlock.Builder = apply {
    val bindFunction = typeAffinity.getBindFunction()
    addStatement("$bindFunction($index, $parameter)")
}

private fun KSDeclaration.getConverterToAffinity(typeAffinity: ColumnInfo.TypeAffinity) {

}

private fun ColumnInfo.TypeAffinity.getBindFunction(): String = when (this) {
    ColumnInfo.TypeAffinity.INTEGER -> SqlPreparedStatement::bindLong.name
    ColumnInfo.TypeAffinity.NUMERIC -> SqlPreparedStatement::bindString.name
    ColumnInfo.TypeAffinity.REAL -> SqlPreparedStatement::bindDouble.name
    ColumnInfo.TypeAffinity.TEXT -> SqlPreparedStatement::bindString.name
    ColumnInfo.TypeAffinity.UNDEFINED,
    ColumnInfo.TypeAffinity.NONE -> error("Can't find bind function for this type $this")
}
