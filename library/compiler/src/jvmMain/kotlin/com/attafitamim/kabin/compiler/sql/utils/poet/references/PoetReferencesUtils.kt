package com.attafitamim.kabin.compiler.sql.utils.poet.references

import com.attafitamim.kabin.compiler.sql.utils.poet.toCamelCase
import com.attafitamim.kabin.compiler.sql.utils.poet.toLowerCamelCase
import com.attafitamim.kabin.core.table.KabinTable
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.asClassName

fun ColumnAdapterReference.getMapperPropertyName() = buildString {
    append(
        kotlinType.simpleName.toLowerCamelCase(),
        affinityType.name.lowercase().toCamelCase()
    )
}

fun KSClassDeclaration.getMapperPropertyName() = buildString {
    append(
        simpleName.asString().toLowerCamelCase(),
        KabinTable.Mapper::class.asClassName().simpleName.toCamelCase()
    )
}
