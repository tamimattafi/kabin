package com.attafitamim.kabin.compiler.sql.utils.sql

import com.attafitamim.kabin.annotations.ColumnInfo
import com.attafitamim.kabin.compiler.sql.syntax.SQLBuilder
import com.attafitamim.kabin.processor.utils.classDeclaration
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName

val typesMap: Map<TypeName, ColumnInfo.TypeAffinity> = mapOf(
    Boolean::class.asClassName() to ColumnInfo.TypeAffinity.INTEGER,
    Byte::class.asClassName() to ColumnInfo.TypeAffinity.INTEGER,
    Short::class.asClassName() to ColumnInfo.TypeAffinity.INTEGER,
    Int::class.asClassName() to ColumnInfo.TypeAffinity.INTEGER,
    Long::class.asClassName() to ColumnInfo.TypeAffinity.INTEGER,
    Float::class.asClassName() to ColumnInfo.TypeAffinity.REAL,
    Double::class.asClassName() to ColumnInfo.TypeAffinity.REAL,
    String::class.asClassName() to ColumnInfo.TypeAffinity.TEXT,
    ByteArray::class.asClassName() to ColumnInfo.TypeAffinity.NONE
)

val KSType.sqlType: ColumnInfo.TypeAffinity
    get() {
        val typeName = classDeclaration.toClassName()
        return typesMap[typeName]
            ?: fallbackSqlType
            ?: ColumnInfo.TypeAffinity.TEXT
    }

val KSType.fallbackSqlType: ColumnInfo.TypeAffinity? get() =
    when (classDeclaration.classKind) {
        ClassKind.ENUM_ENTRY,
        ClassKind.ENUM_CLASS -> ColumnInfo.TypeAffinity.TEXT
        ClassKind.INTERFACE,
        ClassKind.CLASS,
        ClassKind.OBJECT,
        ClassKind.ANNOTATION_CLASS -> null
    }

fun buildSQLQuery(
    includeStatementSeparator: Boolean = false,
    builder: SQLBuilder.() -> Unit
): String = SQLBuilder().appendStatement(
    includeStatementSeparator,
    builder
).raw
