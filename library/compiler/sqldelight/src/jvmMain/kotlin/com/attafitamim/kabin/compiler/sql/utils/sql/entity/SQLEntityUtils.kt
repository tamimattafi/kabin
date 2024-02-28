package com.attafitamim.kabin.compiler.sql.utils.sql.entity

import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.CREATE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.DELETE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.DROP
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.EXISTS
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.FROM
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.IF
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.NOT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.TABLE
import com.attafitamim.kabin.compiler.sql.utils.sql.buildSQLQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.column.appendColumnDefinition
import com.attafitamim.kabin.compiler.sql.utils.sql.column.appendPrimaryKeysDefinition
import com.attafitamim.kabin.compiler.sql.utils.sql.index.appendForeignKeyDefinition
import com.attafitamim.kabin.compiler.sql.utils.sql.index.getCreationQuery
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.specs.entity.EntitySpec


val EntitySpec.tableCreationQuery: String get() = buildSQLQuery {
    val foreignKeys = foreignKeys

    val actualColumns = columns.filter { columnSpec ->
        columnSpec.ignoreSpec == null && !ignoredColumns.contains(columnSpec.name)
    }

    val hasSinglePrimaryKey = primaryKeys.size <= 1
    val hasForeignKeys = foreignKeys.isNullOrEmpty()

    CREATE; TABLE; IF; NOT; EXISTS(tableName).wrap {
        actualColumns.forEachIndexed { index, columnSpec ->
            val isLastStatement = hasSinglePrimaryKey
                    && index == actualColumns.lastIndex
                    && !hasForeignKeys

            appendColumnDefinition(
                columnSpec,
                hasSinglePrimaryKey,
                isLastStatement
            )
        }

        if (!hasSinglePrimaryKey) {
            appendPrimaryKeysDefinition(primaryKeys)
        }

        foreignKeys?.forEachIndexed { index, foreignKeySpec ->
            val isLastStatement = index == foreignKeys.lastIndex
            appendForeignKeyDefinition(foreignKeySpec, isLastStatement)
        }
    }
}

val EntitySpec.tableDropQuery: String get() = buildSQLQuery {
    DROP; TABLE; IF; EXISTS(tableName)
}

val EntitySpec.tableClearQuery: String get() = buildSQLQuery {
    DELETE; FROM(tableName)
}

fun EntitySpec.getIndicesCreationQueries(options: KabinOptions): List<String>? {
    val prefix = options.getOrDefault(KabinOptions.Key.INDEX_NAME_PREFIX)
    return indices?.map { indexSpec ->
        indexSpec.getCreationQuery(tableName, prefix)
    }
}