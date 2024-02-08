package com.attafitamim.kabin.specs.entity

import com.attafitamim.kabin.annotations.index.ForeignKey
import com.attafitamim.kabin.annotations.index.Index
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.core.ClassSpec

data class EntitySpec(
    val classSpec: ClassSpec,
    val tableName: String?,
    val indices: List<Index>,
    val inheritSuperIndices: Boolean,
    val primaryKeys: List<String>,
    val foreignKeys: List<ForeignKey>,
    val ignoredColumns: List<String>,
    val columns: List<ColumnSpec>
)
