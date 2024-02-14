package com.attafitamim.kabin.specs.column

import com.attafitamim.kabin.annotations.column.ColumnInfo
import kotlin.reflect.KProperty

data class ColumnSpec(
    val property: KProperty<*>,
    val name: String?,
    val typeAffinity: ColumnInfo.TypeAffinity,
    val index: Boolean,
    val collate: ColumnInfo.Collate,
    val defaultValue: String?
)
