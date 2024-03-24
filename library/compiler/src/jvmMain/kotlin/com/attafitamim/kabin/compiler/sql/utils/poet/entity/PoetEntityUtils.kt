package com.attafitamim.kabin.compiler.sql.utils.poet.entity

import app.cash.sqldelight.db.SqlCursor
import com.attafitamim.kabin.annotations.ColumnInfo
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName

val supportedAffinity: Map<ColumnInfo.TypeAffinity, TypeName> = mapOf(
    ColumnInfo.TypeAffinity.INTEGER to Long::class.asClassName(),
    ColumnInfo.TypeAffinity.TEXT to String::class.asClassName(),
    ColumnInfo.TypeAffinity.NONE to ByteArray::class.asClassName(),
    ColumnInfo.TypeAffinity.REAL to Double::class.asClassName()
)

val supportedParsers: Map<TypeName, String> = mapOf(
    Long::class.asClassName() to SqlCursor::getLong.name,
    Double::class.asClassName() to SqlCursor::getDouble.name,
    String::class.asClassName() to SqlCursor::getString.name,
    ByteArray::class.asClassName() to SqlCursor::getBytes.name,
    Boolean::class.asClassName() to SqlCursor::getBoolean.name
)

fun ColumnInfo.TypeAffinity.getParseFunction(): String = when (this) {
    ColumnInfo.TypeAffinity.INTEGER -> SqlCursor::getLong.name
    ColumnInfo.TypeAffinity.NUMERIC -> SqlCursor::getString.name
    ColumnInfo.TypeAffinity.REAL -> SqlCursor::getDouble.name
    ColumnInfo.TypeAffinity.TEXT,
    ColumnInfo.TypeAffinity.UNDEFINED -> SqlCursor::getString.name
    ColumnInfo.TypeAffinity.NONE -> SqlCursor::getBytes.name
}
