package com.attafitamim.kabin.compiler.sql.utils.index

import com.attafitamim.kabin.annotations.index.Index
import com.attafitamim.kabin.compiler.sql.syntax.SQLBuilder
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.ASC
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.CREATE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.DESC
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.EXISTS
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.IF
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.INDEX
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.NOT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.ON
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.Sign.NAME_SEPARATOR
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.UNIQUE
import com.attafitamim.kabin.compiler.sql.utils.sql.buildSQLQuery
import com.attafitamim.kabin.specs.index.IndexSpec

fun IndexSpec.getActualName(
    tableName: String,
    indexNamePrefix: String
): String = name ?: buildString {
    append(indexNamePrefix, NAME_SEPARATOR, tableName)

    columns?.forEach { column ->
        append(NAME_SEPARATOR, column)
    }
}

fun IndexSpec.getCreationQuery(
    tableName: String,
    indexNamePrefix: String
): String = buildSQLQuery {
    val columns = requireNotNull(columns)
    val name = name ?: getActualName(tableName, indexNamePrefix)

    CREATE; if (unique) UNIQUE; INDEX; IF; NOT; EXISTS(name); ON(tableName).wrap {
        columns.forEachIndexed { index, column ->
            val isLastStatement = index == columns.lastIndex
            val order = orders?.get(index)
            appendStatement(!isLastStatement) {
                append(column)
                append(order)
            }
        }
    }
}

fun SQLBuilder.append(order: Index.Order?) = when (order) {
    Index.Order.ASC -> ASC
    Index.Order.DESC -> DESC
    null -> this
}
