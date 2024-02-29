package com.attafitamim.kabin.compiler.sql.utils.poet.references

import com.attafitamim.kabin.compiler.sql.utils.poet.toCamelCase
import com.attafitamim.kabin.compiler.sql.utils.poet.toLowerCamelCase
import com.attafitamim.kabin.core.table.KabinTable
import com.squareup.kotlinpoet.asClassName

fun ColumnAdapterReference.getPropertyName() = buildString {
    append(
        kotlinType.simpleName.toLowerCamelCase(),
        affinityType.name.lowercase().toCamelCase()
    )
}

fun MapperReference.getPropertyName() = buildString {
    append(
        entityType.simpleName.toLowerCamelCase(),
        KabinTable.Mapper::class.asClassName().simpleName.toCamelCase()
    )
}
