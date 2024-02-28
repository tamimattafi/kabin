package com.attafitamim.kabin.compiler.sql.syntax

import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.Sign.FUNCTION_CLOSE_PARENTHESES
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.Sign.FUNCTION_OPEN_PARENTHESES
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.Sign.SEPARATOR
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.Sign.STATEMENT_SEPARATOR
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.Sign.VALUE_ESCAPING

class SQLBuilder {

    private val builder = StringBuilder()

    val raw get() = builder.trim().toString()

    operator fun invoke(
        query: String,
        includeSeparator: Boolean = true
    ): SQLBuilder = apply {
        builder.append(query)

        if (includeSeparator) {
            builder.append(SEPARATOR)
        }
    }

    fun append(
        vararg queries: String,
        includeSeparator: Boolean = true
    ): SQLBuilder = apply {
        queries.forEach { query ->
            append(query, includeSeparator)
        }
    }

    fun append(
        query: String,
        includeSeparator: Boolean = true
    ): SQLBuilder = apply {
        ensureEndsWithSeparator()

        builder.append(query)

        if (includeSeparator) {
            builder.append(SEPARATOR)
        }
    }

    fun appendValue(value: String): SQLBuilder = apply {
        ensureEndsWithSeparator()

        builder.append(
            VALUE_ESCAPING,
            value,
            VALUE_ESCAPING
        )
    }

    fun appendStatement(
        includeStatementSeparator: Boolean = false,
        onAppend: SQLBuilder.() -> Unit
    ): SQLBuilder = apply {
        ensureEndsWithSeparator()

        val newBuilder = SQLBuilder().apply(onAppend)
        val rawStatement = newBuilder.raw

        builder.append(rawStatement)

        if (includeStatementSeparator) {
            builder.append(STATEMENT_SEPARATOR)
        }
    }

    fun wrap(onAppend: SQLBuilder.() -> Unit): SQLBuilder = apply {
        ensureEndsWithSeparator()

        val newBuilder = SQLBuilder().apply(onAppend)
        val rawStatement = newBuilder.raw

        builder.append(
            FUNCTION_OPEN_PARENTHESES,
            rawStatement,
            FUNCTION_CLOSE_PARENTHESES
        )
    }

    private fun ensureEndsWithSeparator() {
        if (builder.isNotBlank() && !builder.endsWith(SEPARATOR)) {
            builder.append(SEPARATOR)
        }
    }
}
