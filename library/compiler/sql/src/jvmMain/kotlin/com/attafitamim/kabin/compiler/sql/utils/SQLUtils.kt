package com.attafitamim.kabin.compiler.sql.utils

import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.compiler.sql.syntax.SQL_COLUMN_AUTO_GENERATE
import com.attafitamim.kabin.compiler.sql.syntax.SQL_COLUMN_CREATION_SEPARATOR
import com.attafitamim.kabin.compiler.sql.syntax.SQL_COLUMN_PRIMARY_KEY
import com.attafitamim.kabin.compiler.sql.syntax.SQL_CREATE_TABLE_TEMPLATE
import com.attafitamim.kabin.compiler.sql.syntax.SQL_SEPARATOR
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
        val columns = columns.joinToString(
            separator = SQL_COLUMN_CREATION_SEPARATOR,
            transform = ColumnSpec::sqlCreationQuery
        )

        return String.format(
            SQL_CREATE_TABLE_TEMPLATE,
            tableName,
            columns
        )
    }

val ColumnSpec.sqlCreationQuery: String get() = buildString {
    val type = sqlType
    append(actualName, SQL_SEPARATOR, type.name)

    primaryKeySpec?.let { spec ->
        append(SQL_SEPARATOR, SQL_COLUMN_PRIMARY_KEY)

        if (spec.autoGenerate) {
            append(SQL_SEPARATOR, SQL_COLUMN_AUTO_GENERATE)
        }
    }
}
