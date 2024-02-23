package com.attafitamim.kabin.compiler.sql.utils

import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.compiler.sql.syntax.SQL_COLUMN_AUTO_GENERATE
import com.attafitamim.kabin.compiler.sql.syntax.SQL_COLUMN_DEFAULT_VALUE
import com.attafitamim.kabin.compiler.sql.syntax.SQL_COLUMN_NOT_NULL
import com.attafitamim.kabin.compiler.sql.syntax.SQL_COLUMN_PRIMARY_KEY
import com.attafitamim.kabin.compiler.sql.syntax.SQL_CREATE_TABLE_TEMPLATE
import com.attafitamim.kabin.compiler.sql.syntax.SQL_SEPARATOR
import com.attafitamim.kabin.compiler.sql.syntax.SQL_STATEMENT_SEPARATOR
import com.attafitamim.kabin.compiler.sql.syntax.SQL_VALUE_ESCAPING
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

val typesMap = mapOf(
    Int::class.qualifiedName to ColumnInfo.TypeAffinity.INTEGER,
    Long::class.qualifiedName to ColumnInfo.TypeAffinity.INTEGER,
    Float::class.qualifiedName to ColumnInfo.TypeAffinity.REAL,
    Double::class.qualifiedName to ColumnInfo.TypeAffinity.REAL,
    String::class.qualifiedName to ColumnInfo.TypeAffinity.TEXT
)

val ColumnSpec.sqlType: ColumnInfo.TypeAffinity
    get() = when (val type = typeAffinity) {
    ColumnInfo.TypeAffinity.INTEGER,
    ColumnInfo.TypeAffinity.NUMERIC,
    ColumnInfo.TypeAffinity.REAL,
    ColumnInfo.TypeAffinity.TEXT,
    ColumnInfo.TypeAffinity.NONE -> type
    ColumnInfo.TypeAffinity.UNDEFINED,
    null -> declaration.sqlType
}

val KSPropertyDeclaration.sqlType: ColumnInfo.TypeAffinity
    get() {
        val qualifiedName = type.resolve().declaration.run {
            qualifiedName ?: simpleName
        }.asString()

        return typesMap.getValue(qualifiedName)
    }

val EntitySpec.sqlCreationQuery: String
    get() {
        val tableName = actualTableName
        val primaryKeys = HashSet(primaryKeys?.toSet().orEmpty())
        val ignoredColumns = ignoredColumns?.toSet().orEmpty()

        val actualColumns = columns.filter { columnSpec ->
            if (columnSpec.primaryKeySpec != null) {
                primaryKeys.add(columnSpec.actualName)
            }

            columnSpec.ignoreSpec == null && !ignoredColumns.contains(columnSpec.actualName)
        }

        val hasSinglePrimaryKey = primaryKeys.size <= 1

        val columnDefinitions = actualColumns.joinToString(
            SQL_STATEMENT_SEPARATOR
        ) { columnSpec ->
            columnSpec.getSqlDefinition(hasSinglePrimaryKey)
        }

        return String.format(
            SQL_CREATE_TABLE_TEMPLATE,
            tableName,
            columnDefinitions
        )
    }

fun ColumnSpec.getSqlDefinition(
    includePrimaryKeyDefinition: Boolean
): String = buildString {
    val type = sqlType
    val isNullable = declaration.type.resolve().isMarkedNullable

    append(actualName, SQL_SEPARATOR, type.name)

    primaryKeySpec?.let { spec ->
        if (includePrimaryKeyDefinition) {
            append(SQL_SEPARATOR, SQL_COLUMN_PRIMARY_KEY)
        }

        if (spec.autoGenerate) {
            append(SQL_SEPARATOR, SQL_COLUMN_AUTO_GENERATE)
        }
    }

    if (!isNullable) {
        append(SQL_SEPARATOR, SQL_COLUMN_NOT_NULL)
    }

    if (!defaultValue.isNullOrBlank()) {
        append(
            SQL_SEPARATOR,
            SQL_COLUMN_DEFAULT_VALUE,
            SQL_SEPARATOR,
            SQL_VALUE_ESCAPING,
            defaultValue,
            SQL_VALUE_ESCAPING
        )
    }
}
