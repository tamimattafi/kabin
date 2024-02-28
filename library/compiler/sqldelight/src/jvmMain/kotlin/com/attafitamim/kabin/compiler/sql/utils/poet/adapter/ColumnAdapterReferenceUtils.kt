package com.attafitamim.kabin.compiler.sql.utils.poet.adapter

import com.attafitamim.kabin.compiler.sql.utils.poet.CONSTANT_SEPARATOR_SIGN

fun ColumnAdapterReference.getPropertyName() = buildString {
    append(kotlinType.simpleName, CONSTANT_SEPARATOR_SIGN, affinityType.name)
}
