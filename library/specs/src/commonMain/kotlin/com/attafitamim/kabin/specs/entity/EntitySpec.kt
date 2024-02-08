package com.attafitamim.kabin.specs.entity

import com.attafitamim.kabin.annotations.index.ForeignKey
import com.attafitamim.kabin.annotations.index.Index
import com.attafitamim.kabin.specs.column.ColumnSpec
import kotlin.reflect.KClass

data class EntitySpec(
    val clazz: KClass<*>,
    val tableName: String?,
    val indices: List<Index>,
    val inheritSuperIndices: Boolean,
    val primaryKeys: List<String>,
    val foreignKeys: List<ForeignKey>,
    val ignoredColumns: List<String>,
    val columns: List<ColumnSpec>
)
