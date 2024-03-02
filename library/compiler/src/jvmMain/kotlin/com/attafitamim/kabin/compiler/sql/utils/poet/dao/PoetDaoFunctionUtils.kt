package com.attafitamim.kabin.compiler.sql.utils.poet.dao

import app.cash.sqldelight.db.SqlPreparedStatement
import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.compiler.sql.generator.references.ColumnAdapterReference
import com.attafitamim.kabin.compiler.sql.utils.poet.entity.supportedAffinity
import com.attafitamim.kabin.compiler.sql.utils.poet.qualifiedNameString
import com.attafitamim.kabin.compiler.sql.utils.sql.sqlType
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName

val supportedBinders = mapOf(
    Long::class.qualifiedName to SqlPreparedStatement::bindLong.name,
    Double::class.qualifiedName to SqlPreparedStatement::bindDouble.name,
    String::class.qualifiedName to SqlPreparedStatement::bindString.name,
    ByteArray::class.qualifiedName to SqlPreparedStatement::bindBytes.name,
    Boolean::class.qualifiedName to SqlPreparedStatement::bindBoolean.name
)

fun KSDeclaration.needsConvert(
    typeAffinity: ColumnInfo.TypeAffinity?
): Boolean {
    val isSameAffinity = typeAffinity == null ||
            typeAffinity == ColumnInfo.TypeAffinity.UNDEFINED ||
            typeAffinity == sqlType

    return !isSameAffinity || !supportedBinders.containsKey(qualifiedNameString)
}

fun KSClassDeclaration.getAdapterReference(
    typeAffinity: ColumnInfo.TypeAffinity?
): ColumnAdapterReference? {
    if (!needsConvert(typeAffinity)) {
        return null
    }

    val actualAffinity = typeAffinity ?: sqlType
    val affinityType = supportedAffinity.getValue(actualAffinity).asClassName()
    return ColumnAdapterReference(
        affinityType,
        toClassName()
    )
}
