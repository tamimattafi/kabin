package com.attafitamim.kabin.compiler.sql.utils.sql.entity

import com.attafitamim.kabin.compiler.sql.syntax.SQLBuilder
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
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.column.ColumnTypeSpec
import com.attafitamim.kabin.specs.entity.EntitySpec


val EntitySpec.tableCreationQuery: String get() = buildSQLQuery {
    val foreignKeys = foreignKeys

    val hasSinglePrimaryKey = primaryKeys.size <= 1
    val hasForeignKeys = foreignKeys.isNullOrEmpty()

    CREATE; TABLE; IF; NOT; EXISTS(tableName).wrap {
        appendColumnsDefinition(
            columns,
            hasSinglePrimaryKey,
            hasForeignKeys
        )

        if (!hasSinglePrimaryKey) {
            appendPrimaryKeysDefinition(primaryKeys)
        }

        foreignKeys?.forEachIndexed { index, foreignKeySpec ->
            val isLastStatement = index == foreignKeys.lastIndex
            appendForeignKeyDefinition(foreignKeySpec, isLastStatement)
        }
    }
}

fun SQLBuilder.appendColumnsDefinition(
    columns: List<ColumnSpec>,
    hasSinglePrimaryKey: Boolean,
    hasForeignKeys: Boolean
) {
    columns.forEachIndexed { index, columnSpec ->
        when (val dataType = columnSpec.typeSpec.dataType) {
            is ColumnTypeSpec.DataType.Class -> {
                val isLastStatement = hasSinglePrimaryKey
                        && index == columns.lastIndex
                        && !hasForeignKeys

                appendColumnDefinition(
                    columnSpec,
                    hasSinglePrimaryKey,
                    isLastStatement
                )
            }

            is ColumnTypeSpec.DataType.Embedded -> {
                appendColumnsDefinition(
                    dataType.columns,
                    hasSinglePrimaryKey,
                    hasForeignKeys
                )
            }
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