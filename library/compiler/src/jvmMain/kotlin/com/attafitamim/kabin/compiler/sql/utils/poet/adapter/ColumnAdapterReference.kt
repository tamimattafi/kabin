package com.attafitamim.kabin.compiler.sql.utils.poet.adapter

import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.squareup.kotlinpoet.ClassName

data class ColumnAdapterReference(
    val affinityType: ColumnInfo.TypeAffinity,
    val kotlinType: ClassName
)
