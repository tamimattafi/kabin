package com.attafitamim.kabin.compiler.sql.utils

import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.CREATE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.DELETE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.DROP
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.EXITS
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.FROM
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.IF
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.NOT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.TABLE
import com.attafitamim.kabin.specs.entity.EntitySpec

val EntitySpec.actualTableName: String get() = tableName ?: declaration.simpleName.asString()

val EntitySpec.sqlCreationQuery: String get() = buildSQLQuery {
    CREATE; TABLE; IF; NOT; EXITS(actualTableName)

    val primaryKeys = LinkedHashSet(primaryKeys.orEmpty())
    val ignoredColumns = LinkedHashSet(ignoredColumns.orEmpty())

    val actualColumns = columns.filter { columnSpec ->
        if (columnSpec.primaryKeySpec != null) {
            primaryKeys.add(columnSpec.actualName)
        }

        columnSpec.ignoreSpec == null && !ignoredColumns.contains(columnSpec.actualName)
    }

    val hasSinglePrimaryKey = primaryKeys.size <= 1
    wrap {
        actualColumns.forEachIndexed { index, columnSpec ->
            val isLastStatement = hasSinglePrimaryKey && index == actualColumns.lastIndex
            appendColumnDefinition(
                columnSpec,
                hasSinglePrimaryKey,
                isLastStatement
            )
        }

        if (!hasSinglePrimaryKey) {
            appendPrimaryKeysDefinition(primaryKeys)
        }
    }
}

val EntitySpec.sqlDropQuery: String get() = buildSQLQuery {
    DROP; TABLE; IF; EXITS(actualTableName)
}

val EntitySpec.sqlClearQuery: String get() = buildSQLQuery {
    DELETE; FROM(actualTableName)
}