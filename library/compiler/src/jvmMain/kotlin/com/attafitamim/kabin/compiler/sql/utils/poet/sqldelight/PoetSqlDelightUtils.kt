package com.attafitamim.kabin.compiler.sql.utils.poet.sqldelight

import com.attafitamim.kabin.compiler.sql.syntax.SQLQuery
import com.attafitamim.kabin.specs.dao.DataTypeSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec


const val EXECUTE_FUNCTION = "execute"
const val EXECUTE_QUERY_FUNCTION = "executeQuery"

private val generatedIds = HashMap<String, Int>()
private val generatedHashCodes = HashMap<Int, String>()

private const val MAX_UNIQUE_CYCLES = 100
private const val CYCLES_START_INDEX = 0
private const val HASH_CODE_INTERVAL = 31

fun SQLQuery.getQueryIdentifier(): Int? = when (this) {
    is SQLQuery.Columns -> value.getUniqueHashCode()
    is SQLQuery.Parameters -> value.getUniqueHashCode()
    is SQLQuery.Raw -> null
}

// TODO: optimize this
fun String.getUniqueHashCode(): Int? {
    val generatedId = generatedIds[this]
    if (generatedId != null) {
        return generatedId
    }

    var cycle = CYCLES_START_INDEX
    while (cycle <= MAX_UNIQUE_CYCLES) {
        val originalHashCode = hashCode()
        val hashCode = if (cycle == CYCLES_START_INDEX) {
            originalHashCode
        } else {
            originalHashCode * (cycle * HASH_CODE_INTERVAL)
        }

        val valueLinkedToHashCode = generatedHashCodes[hashCode]
        if (valueLinkedToHashCode == null || valueLinkedToHashCode == this) {
            generatedIds[this] = hashCode
            generatedHashCodes[hashCode] = this
            return hashCode
        }

        cycle++
    }

    return null
}


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
    val logic = "driver.execute($identifier,·%P,·$parametersSize)"
    addStatement(logic, sql)
}

fun FunSpec.Builder.addDriverRawQueryCode(
    query: SQLQuery.Raw,
    function: String,
    binderCode: (CodeBlock.Builder.() -> Unit)? = null
): FunSpec.Builder = apply {
    val identifier = query.getQueryIdentifier()
    val logic = if (function == EXECUTE_QUERY_FUNCTION) {
        """
        |val result = driver.executeQuery(
        |    identifier = $identifier,
        |    sql = %L,
        |    mapper = mapper,
        |    parameters = 0
        |)
        """.trimMargin()
    } else {
        """
        |driver.execute(
        |    identifier = $identifier,
        |    sql = %L,
        |    parameters = 0
        |)
        """.trimMargin()
    }

    val codeBlockBuilder = CodeBlock.builder()
        .addStatement(logic, query.value)

    if (binderCode != null) {
        codeBlockBuilder.binderCode()
    }

    if (function == EXECUTE_QUERY_FUNCTION) {
        codeBlockBuilder.addStatement("return result")
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
    val addedConstants = HashSet<String>()
    query.queryParameters.forEach { queryParameter ->
        if (queryParameter.spec.typeSpec.dataType is DataTypeSpec.DataType.Wrapper) {
            val sizeConstant = "${queryParameter.spec.name}Size"
            if (!addedConstants.contains(sizeConstant)) {
                if (queryParameter.spec.typeSpec.isNullable) {
                    addStatement("val $sizeConstant = ${queryParameter.spec.name}.orEmpty().size")
                } else {
                    addStatement("val $sizeConstant = ${queryParameter.spec.name}.size")
                }

                addedConstants.add(sizeConstant)
            }

            if (queryParameter.previousKeyword == "IN") {
                val argumentsConstant = "${queryParameter.spec.name}Arguments"
                if (!addedConstants.contains(argumentsConstant)) {
                    addStatement("val $argumentsConstant = createNullableArguments(${queryParameter.spec.name}?.size)")
                    addedConstants.add(argumentsConstant)
                }

                if (sizeExpression.isNotEmpty()) {
                    sizeExpression.append(" + ")
                }

                sizeExpression.append(sizeConstant)
            } else {
                val parameterConstant = "${queryParameter.spec.name}Parameter"
                if (!addedConstants.contains(parameterConstant)) {
                    addStatement("val $parameterConstant = createNullableParameter(${queryParameter.spec.name}?.size)")
                    addedConstants.add(parameterConstant)
                }
            }
        } else {
            simpleParametersSize++
        }
    }

    if (sizeExpression.isNotEmpty()) {
        sizeExpression.append(" + ")
    }

    sizeExpression.append(simpleParametersSize)
    val codeBlockBuilder = CodeBlock.builder()
        .addStatement("val internalQuerySql = %P", query.value)
        .addStatement("val internalQueryParametersCount = $sizeExpression")

    val originalIdentifier = query.getQueryIdentifier()
    val identifier = if (addedConstants.isEmpty()) {
        originalIdentifier
    } else {
        null
    }

    val logic = if (function == "executeQuery") {
        """
        |val result = driver.executeQuery(
        |    identifier = $identifier,
        |    sql = internalQuerySql,
        |    mapper = mapper,
        |    parameters = internalQueryParametersCount
        |)
        """.trimMargin()
    } else {
        """
        |driver.execute(
        |    identifier = $identifier,
        |    sql = internalQuerySql,
        |    parameters = internalQueryParametersCount
        |)
        """.trimMargin()
    }

    codeBlockBuilder.addStatement(logic, query.value)

    if (binderCode != null) {
        codeBlockBuilder.binderCode()
    }

    if (query.mutatedKeys.isNotEmpty()) {
        val notifyIdentifier = identifier ?: -1
        codeBlockBuilder.beginControlFlow("notifyQueries($notifyIdentifier) { emit ->")

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

    val identifier = query.getQueryIdentifier()
    val logic = if (function == EXECUTE_QUERY_FUNCTION) {
        """
        |val result = driver.executeQuery(
        |    identifier = $identifier,
        |    sql = %P,
        |    mapper = mapper,
        |    parameters = ${query.parametersSize}
        |)
        """.trimMargin()
    } else {
        """
        |driver.execute(
        |    identifier = $identifier,
        |    sql = %P,
        |    parameters = ${query.parametersSize}
        |)
        """.trimMargin()
    }

    codeBlockBuilder.addStatement(logic, query.value)

    if (binderCode != null) {
        codeBlockBuilder.binderCode()
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
