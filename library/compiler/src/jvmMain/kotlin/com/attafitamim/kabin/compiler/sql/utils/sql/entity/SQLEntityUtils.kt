package com.attafitamim.kabin.compiler.sql.utils.sql.entity

import com.attafitamim.kabin.compiler.sql.syntax.SQLBuilder
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.CREATE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.DELETE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.DROP
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.EQUALS
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.EXISTS
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.FROM
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.FTS4
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.IF
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.NOT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.TABLE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.USING
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.VIRTUAL
import com.attafitamim.kabin.compiler.sql.utils.sql.buildSQLQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.column.appendColumnDefinition
import com.attafitamim.kabin.compiler.sql.utils.sql.column.appendPrimaryKeysDefinition
import com.attafitamim.kabin.compiler.sql.utils.sql.index.appendForeignKeyDefinition
import com.attafitamim.kabin.compiler.sql.utils.sql.index.getCreationQuery
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.column.ColumnTypeSpec
import com.attafitamim.kabin.specs.entity.EntitySearchSpec
import com.attafitamim.kabin.specs.entity.EntitySpec

val EntitySpec.tableCreationQuery: String get() {
    val searchSpec = searchSpec
    return if (searchSpec != null) {
        getFts4CreationQuery(searchSpec)
    } else {
        simpleCreationQuery
    }
}

val EntitySpec.simpleCreationQuery: String get() = buildSQLQuery {
    val foreignKeys = foreignKeys

    val hasSinglePrimaryKey = primaryKeys.size <= 1
    val hasForeignKeys = !foreignKeys.isNullOrEmpty()

    CREATE; TABLE; IF; NOT; EXISTS(tableName).wrap {
        appendColumnsDefinition(
            columns,
            hasSinglePrimaryKey,
            !hasForeignKeys
        )

        if (!hasSinglePrimaryKey) {
            appendPrimaryKeysDefinition(
                primaryKeys,
                !hasForeignKeys
            )
        }

        foreignKeys?.forEachIndexed { index, foreignKeySpec ->
            val isLastStatement = index == foreignKeys.lastIndex
            appendForeignKeyDefinition(foreignKeySpec, isLastStatement)
        }
    }
}

fun EntitySpec.getFts4CreationQuery(searchSpec: EntitySearchSpec): String = buildSQLQuery {
    val foreignKeys = foreignKeys

    val hasSinglePrimaryKey = primaryKeys.size <= 1
    CREATE; VIRTUAL; TABLE; IF; NOT; EXISTS(tableName); USING; FTS4.wrap {
        appendColumnsDefinition(
            columns,
            hasSinglePrimaryKey,
            isLastColumnsAppend = false
        )

        if (!hasSinglePrimaryKey) {
            appendPrimaryKeysDefinition(
                primaryKeys,
                isLastStatement = false
            )
        }

        foreignKeys?.forEach { foreignKeySpec ->
            appendForeignKeyDefinition(
                foreignKeySpec,
                isLastStatement = false
            )
        }

        appendStatement(includeStatementSeparator = false) {
            append("content"); EQUALS(searchSpec.contentEntity.tableName)
        }
    }
}

fun SQLBuilder.appendColumnsDefinition(
    columns: List<ColumnSpec>,
    hasSinglePrimaryKey: Boolean,
    isLastColumnsAppend: Boolean
) {
    val flatColumns = getFlatColumns(columns)
    flatColumns.forEachIndexed { index, columnSpec ->
        val isLastStatement = hasSinglePrimaryKey
                && index == flatColumns.lastIndex
                && isLastColumnsAppend

        when (columnSpec.typeSpec.dataType) {
            is ColumnTypeSpec.DataType.Class -> {
                appendColumnDefinition(
                    columnSpec,
                    hasSinglePrimaryKey,
                    isLastStatement
                )
            }

            is ColumnTypeSpec.DataType.Embedded -> {
                return@forEachIndexed
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

fun getFlatColumns(columns: List<ColumnSpec>): List<ColumnSpec> {
    val parameters = ArrayList<ColumnSpec>()
    columns.forEach { columnSpec ->
        when (val type = columnSpec.typeSpec.dataType) {
            is ColumnTypeSpec.DataType.Class -> {
                parameters.add(columnSpec)
            }

            is ColumnTypeSpec.DataType.Embedded -> {
                parameters.addAll(getFlatColumns(type.columns))
            }
        }
    }

    return parameters
}
