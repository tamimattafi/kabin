package com.attafitamim.kabin.compiler.sql.utils.poet.adapter

import com.attafitamim.kabin.compiler.sql.utils.poet.toCamelCase
import com.attafitamim.kabin.compiler.sql.utils.poet.toLowerCamelCase

fun ColumnAdapterReference.getPropertyName() = buildString {
    append(
        kotlinType.simpleName.toLowerCamelCase(),
        affinityType.name.lowercase().toCamelCase()
    )
}
