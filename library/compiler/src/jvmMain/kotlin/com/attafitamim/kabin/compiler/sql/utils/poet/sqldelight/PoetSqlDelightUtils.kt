package com.attafitamim.kabin.compiler.sql.utils.poet.sqldelight

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.attafitamim.kabin.compiler.sql.syntax.SQLQuery
import com.attafitamim.kabin.specs.dao.DataTypeSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec

fun FunSpec.Builder.addDriverQueryCode(
    query: SQLQuery,
    binderCode: (CodeBlock.Builder.() -> Unit)? = null
) = when (query) {
    is SQLQuery.Columns -> addDriverQueryCode(
        query.value.hashCode(),
        query.value,
        query.parametersSize,
        binderCode
    )

    is SQLQuery.Parameters -> addDriverQueryCode(
        query,
        binderCode
    )

    is SQLQuery.Raw -> addDriverRawQueryCode(query.value)
}

fun FunSpec.Builder.addDriverExecutionCode(
    sql: String,
    identifier: Int? = null,
    parametersSize: Int = 0,
    binderCode: (CodeBlock.Builder.() -> Unit)? = null
) = addDriverExecutionCode(
    identifier,
    sql,
    parametersSize,
    binderCode
)

fun FunSpec.Builder.addDriverExecutionCode(
    identifier: Int?,
    sql: String,
    parametersSize: Int = 0,
    binderCode: (CodeBlock.Builder.() -> Unit)? = null
) = apply {
    val executeMethodName = SqlDriver::execute.name
    val awaitMethodName = QueryResult<*>::await.name

    val codeBlockBuilder = CodeBlock.builder()
        .addStatement("driver.$executeMethodName(")
        .addStatement("${identifier.toString()},")
        .addStatement("%P,", sql)
        .addStatement(parametersSize.toString())
        .addStatement(")")

    if (binderCode != null) {
        codeBlockBuilder.binderCode()
    }

    codeBlockBuilder.addStatement(".$awaitMethodName()")
    addCode(codeBlockBuilder.build())
}


fun FunSpec.Builder.addDriverExecutionCode(
    identifier: String?,
    sql: String,
    parametersSize: String,
    binderCode: (CodeBlock.Builder.() -> Unit)? = null
): FunSpec.Builder = apply {
    val executeMethodName = SqlDriver::execute.name
    val awaitMethodName = QueryResult<*>::await.name

    val codeBlockBuilder = CodeBlock.builder()
        .addStatement("driver.$executeMethodName(")
        .addStatement("${identifier.toString()},")
        .addStatement("$sql,")
        .addStatement(parametersSize)
        .addStatement(")")

    if (binderCode != null) {
        codeBlockBuilder.binderCode()
    }

    codeBlockBuilder.addStatement(".$awaitMethodName()")
    addCode(codeBlockBuilder.build())
}


fun FunSpec.Builder.addDriverQueryCode(
    sql: String,
    identifier: Int? = null,
    parametersSize: Int = 0,
    binderCode: (CodeBlock.Builder.() -> Unit)? = null
) = addDriverQueryCode(
    identifier,
    sql,
    parametersSize,
    binderCode
)

fun FunSpec.Builder.addDriverQueryCode(
    identifier: Int?,
    sql: String,
    parametersSize: Int = 0,
    binderCode: (CodeBlock.Builder.() -> Unit)? = null
) = apply {
   val executeMethodName = "executeQuery"

    val logic = """
        val result = driver.$executeMethodName(
            ${identifier.toString()},
            %P,
            mapper,
            $parametersSize
        )
    """.trimIndent()

    val codeBlockBuilder = CodeBlock.builder()
        .addStatement(logic, sql)

    if (binderCode != null) {
        codeBlockBuilder.binderCode()
    }

    codeBlockBuilder.addStatement("return result")
    addCode(codeBlockBuilder.build())
}

fun FunSpec.Builder.addDriverRawQueryCode(
    parameter: String,
    binderCode: (CodeBlock.Builder.() -> Unit)? = null
): FunSpec.Builder = apply {
    val executeMethodName = "executeQuery"

    val logic = """
        val result = driver.$executeMethodName(
            %L.hashCode(),
            %L,
            mapper,
            0
        )
    """.trimIndent()

    val codeBlockBuilder = CodeBlock.builder()
        .addStatement(logic, parameter, parameter)

    if (binderCode != null) {
        codeBlockBuilder.binderCode()
    }

    codeBlockBuilder.addStatement("return result")
    addCode(codeBlockBuilder.build())
}

fun FunSpec.Builder.addDriverQueryCode(
    query: SQLQuery.Parameters,
    binderCode: (CodeBlock.Builder.() -> Unit)? = null
) = apply {
    var hasDynamicParameters = false
    var simpleParametersSize = 0
    //var currentSimpleParametersSize = 0
    val sizeExpression = StringBuilder()

    query.parameters.forEach { daoParameterSpec ->
        if (daoParameterSpec.typeSpec.dataType is DataTypeSpec.DataType.Wrapper) {
            hasDynamicParameters = true
            val parameterAccess = if (daoParameterSpec.typeSpec.isNullable) {
                "${daoParameterSpec.name}.orEmpty().size"
            } else {
                "${daoParameterSpec.name}.size"
            }

            addStatement("val ${daoParameterSpec.name}Indexes = createArguments($parameterAccess)")

            if (sizeExpression.isNotEmpty()) {
                sizeExpression.append(" + ")
            }

            if (daoParameterSpec.typeSpec.isNullable) {
                sizeExpression.append("${daoParameterSpec.name}.orEmpty().size")
            } else {
                sizeExpression.append("${daoParameterSpec.name}.size")
            }
        } else {
            simpleParametersSize++
        }
    }

    if (!hasDynamicParameters) {
        return addDriverQueryCode(
            query.value.hashCode(),
            query.value,
            query.parameters.size,
            binderCode
        )
    }

    if (sizeExpression.isNotEmpty()) {
        sizeExpression.append(" + ")
    }

    sizeExpression.append(simpleParametersSize)

    val codeBlockBuilder = CodeBlock.builder()
        .addStatement("val query = %P", query.value)
        .addStatement("val parametersCount = $sizeExpression")

    val logic = """
        |val result = driver.executeQuery(
        |    query.hashCode(),
        |    query,
        |    mapper,
        |    parametersCount
        |)
    """.trimMargin()

    codeBlockBuilder.addStatement(logic, query.value)

    if (binderCode != null) {
        codeBlockBuilder.binderCode()
    }

    codeBlockBuilder.addStatement("return result")
    addCode(codeBlockBuilder.build())
}
