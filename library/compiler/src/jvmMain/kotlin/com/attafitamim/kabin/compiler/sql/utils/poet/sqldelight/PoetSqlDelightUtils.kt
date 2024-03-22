package com.attafitamim.kabin.compiler.sql.utils.poet.sqldelight

import com.attafitamim.kabin.compiler.sql.syntax.SQLQuery
import com.attafitamim.kabin.specs.dao.DaoParameterSpec
import com.attafitamim.kabin.specs.dao.DataTypeSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec

const val EXECUTE_FUNCTION = "execute"
const val EXECUTE_QUERY_FUNCTION = "executeQuery"

fun FunSpec.Builder.addDriverQueryCode(
    query: SQLQuery,
    function: String = EXECUTE_QUERY_FUNCTION,
    binderCode: (CodeBlock.Builder.() -> Unit)? = null
) = when (query) {
    is SQLQuery.Columns -> addDriverQueryCode(
        query,
        function,
        binderCode
    )

    is SQLQuery.Parameters -> addDriverQueryCode(
        query,
        function,
        binderCode
    )

    is SQLQuery.Raw -> addDriverRawQueryCode(
        query,
        function,
        binderCode
    )
}

fun FunSpec.Builder.addDriverExecutionCode(
    sql: String,
    identifier: Int? = null,
    parametersSize: Int = 0
) = addDriverExecutionCode(
    identifier,
    sql,
    parametersSize
)

fun FunSpec.Builder.addDriverExecutionCode(
    identifier: Int?,
    sql: String,
    parametersSize: Int = 0
) = apply {
    val logic = "driver.execute($identifier,·%P,·$parametersSize).await()"
    addStatement(logic, sql)
}

fun FunSpec.Builder.addDriverRawQueryCode(
    query: SQLQuery.Raw,
    function: String,
    binderCode: (CodeBlock.Builder.() -> Unit)? = null
): FunSpec.Builder = apply {
    val logic = if (function == "executeQuery") {
        """
        |val result = driver.executeQuery(
        |    null,
        |    %L,
        |    mapper,
        |    0
        |)
        """.trimMargin()
    } else {
        """
        |driver.execute(
        |    null,
        |    %L,
        |    0
        |)
        """.trimMargin()
    }

    val codeBlockBuilder = CodeBlock.builder()
        .addStatement(logic, query.value)

    if (binderCode != null) {
        codeBlockBuilder.binderCode()
    }

    if (function == "executeQuery") {
        codeBlockBuilder.addStatement("return result")
    } else {
        codeBlockBuilder.add(".await()")
    }

    addCode(codeBlockBuilder.build())
}

fun FunSpec.Builder.addDriverQueryCode(
    query: SQLQuery.Parameters,
    function: String,
    binderCode: (CodeBlock.Builder.() -> Unit)? = null
) = apply {
    var simpleParametersSize = 0
    val sizeExpression = StringBuilder()

    // TODO: refactor this, use more optimized way
    val handledWrappers = LinkedHashSet<DaoParameterSpec>()
    query.parameters.forEach { daoParameterSpec ->
        if (daoParameterSpec.typeSpec.dataType is DataTypeSpec.DataType.Wrapper) {
            val sizeParameter = "${daoParameterSpec.name}Size"
            if (!handledWrappers.contains(daoParameterSpec)) {
                if (daoParameterSpec.typeSpec.isNullable) {
                    addStatement("val $sizeParameter = ${daoParameterSpec.name}.orEmpty().size")
                } else {
                    addStatement("val $sizeParameter = ${daoParameterSpec.name}.size")
                }

                if (daoParameterSpec.typeSpec.isNullable) {
                    addStatement("val ${daoParameterSpec.name}Indexes = createNullableArguments(${daoParameterSpec.name}?.size)")
                } else {
                    addStatement("val ${daoParameterSpec.name}Indexes = createArguments(${daoParameterSpec.name}.size)")
                }
                handledWrappers.add(daoParameterSpec)
            }

            if (sizeExpression.isNotEmpty()) {
                sizeExpression.append(" + ")
            }

            sizeExpression.append(sizeParameter)
        } else {
            simpleParametersSize++
        }
    }

    if (sizeExpression.isNotEmpty()) {
        sizeExpression.append(" + ")
    }

    sizeExpression.append(simpleParametersSize)
    val codeBlockBuilder = CodeBlock.builder()
        .addStatement("val kabinQuery = %P", query.value)
        .addStatement("val kabinParametersCount = $sizeExpression")

    val identifier = query.hashCode()
    val logic = if (function == "executeQuery") {
        """
        |val result = driver.executeQuery(
        |    $identifier,
        |    kabinQuery,
        |    mapper,
        |    kabinParametersCount
        |)
        """.trimMargin()
    } else {
        """
        |driver.execute(
        |    $identifier,
        |    kabinQuery,
        |    kabinParametersCount
        |)
        """.trimMargin()
    }

    codeBlockBuilder.addStatement(logic, query.value)

    if (binderCode != null) {
        codeBlockBuilder.binderCode()
    }

    if (function == EXECUTE_FUNCTION) {
        codeBlockBuilder.addStatement(".await()")
    }

    if (query.mutatedKeys.isNotEmpty()) {
        codeBlockBuilder.beginControlFlow("notifyQueries($identifier) { emit ->")

        query.mutatedKeys.forEach { key ->
            codeBlockBuilder.addStatement("emit(%S)", key)
        }

        codeBlockBuilder.endControlFlow()
    }

    if (function == EXECUTE_QUERY_FUNCTION) {
        codeBlockBuilder.addStatement("return result")
    }

    addCode(codeBlockBuilder.build())
}

fun FunSpec.Builder.addDriverQueryCode(
    query: SQLQuery.Columns,
    function: String,
    binderCode: (CodeBlock.Builder.() -> Unit)? = null
) = apply {
    val codeBlockBuilder = CodeBlock.builder()

    val identifier = query.hashCode()
    val logic = if (function == "executeQuery") {
        """
        |val result = driver.executeQuery(
        |    $identifier,
        |    %P,
        |    mapper,
        |    ${parameters.size}
        |)
        """.trimMargin()
    } else {
        """
        |driver.execute(
        |    $identifier,
        |    %P,
        |    ${parameters.size}
        |)
        """.trimMargin()
    }

    codeBlockBuilder.addStatement(logic, query.value)

    if (binderCode != null) {
        codeBlockBuilder.binderCode()
    }

    if (function == EXECUTE_FUNCTION) {
        codeBlockBuilder.addStatement(".await()")
    }

    if (query.mutatedKeys.isNotEmpty()) {
        codeBlockBuilder.beginControlFlow("notifyQueries($identifier) { emit ->")

        query.mutatedKeys.forEach { key ->
            codeBlockBuilder.addStatement("emit(%S)", key)
        }

        codeBlockBuilder.endControlFlow()
    }

    if (function == EXECUTE_QUERY_FUNCTION) {
        codeBlockBuilder.addStatement("return result")
    }

    addCode(codeBlockBuilder.build())
}
