package com.attafitamim.kabin.specs.entity

import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.relation.ForeignKeySpec
import com.attafitamim.kabin.specs.index.IndexSpec
import com.google.devtools.ksp.symbol.KSClassDeclaration

data class EntitySpec(
    val declaration: KSClassDeclaration,
    val tableName: String,
    val indices: List<IndexSpec>?,
    val inheritSuperIndices: Boolean,
    val primaryKeys: Set<String>,
    val foreignKeys: List<ForeignKeySpec>?,
    val ignoredColumns: Set<String>,
    val columns: List<ColumnSpec>,
    val searchSpec: EntitySearchSpec?
)
