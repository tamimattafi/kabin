package com.attafitamim.kabin.compiler.sql.utils

import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.compiler.sql.syntax.SQLBuilder
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

val typesMap = mapOf(
    Int::class.qualifiedName to ColumnInfo.TypeAffinity.INTEGER,
    Long::class.qualifiedName to ColumnInfo.TypeAffinity.INTEGER,
    Float::class.qualifiedName to ColumnInfo.TypeAffinity.REAL,
    Double::class.qualifiedName to ColumnInfo.TypeAffinity.REAL,
    String::class.qualifiedName to ColumnInfo.TypeAffinity.TEXT
)

val KSPropertyDeclaration.sqlType: ColumnInfo.TypeAffinity
    get() {
        val qualifiedName = type.resolve().declaration.run {
            qualifiedName ?: simpleName
        }.asString()

        return typesMap.getValue(qualifiedName)
    }

fun buildSQLQuery(
    includeStatementSeparator: Boolean = false,
    builder: SQLBuilder.() -> Unit
): String = SQLBuilder().appendStatement(
    includeStatementSeparator,
    builder
).raw
