package com.attafitamim.kabin.compiler.sql.utils.sql

import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.compiler.sql.syntax.SQLBuilder
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

val typesMap = mapOf(
    Boolean::class.qualifiedName to ColumnInfo.TypeAffinity.INTEGER,
    Byte::class.qualifiedName to ColumnInfo.TypeAffinity.INTEGER,
    Short::class.qualifiedName to ColumnInfo.TypeAffinity.INTEGER,
    Int::class.qualifiedName to ColumnInfo.TypeAffinity.INTEGER,
    Long::class.qualifiedName to ColumnInfo.TypeAffinity.INTEGER,
    Float::class.qualifiedName to ColumnInfo.TypeAffinity.REAL,
    Double::class.qualifiedName to ColumnInfo.TypeAffinity.REAL,
    String::class.qualifiedName to ColumnInfo.TypeAffinity.TEXT,
    ByteArray::class.qualifiedName to ColumnInfo.TypeAffinity.NONE
)

val KSPropertyDeclaration.sqlType: ColumnInfo.TypeAffinity
    get() = type.resolve().declaration.sqlType

val KSDeclaration.sqlType: ColumnInfo.TypeAffinity
    get() {
        val qualifiedName = (qualifiedName ?: simpleName).asString()
        return typesMap.getValue(qualifiedName)
    }

fun buildSQLQuery(
    includeStatementSeparator: Boolean = false,
    builder: SQLBuilder.() -> Unit
): String = SQLBuilder().appendStatement(
    includeStatementSeparator,
    builder
).raw
