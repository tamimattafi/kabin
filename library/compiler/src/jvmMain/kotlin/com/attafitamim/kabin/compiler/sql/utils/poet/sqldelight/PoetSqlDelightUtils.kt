package com.attafitamim.kabin.compiler.sql.utils.poet.sqldelight

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec

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
        .addStatement("%S,", sql)
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
            %S,
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


fun FunSpec.Builder.addDriverQueryCode(
    identifier: String?,
    sql: String,
    parametersSize: String,
    binderCode: (CodeBlock.Builder.() -> Unit)? = null
): FunSpec.Builder = apply {
    val executeMethodName = "executeQuery"

    val logic = """
        val result = driver.$executeMethodName(
            ${identifier.toString()},
            $sql,
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
