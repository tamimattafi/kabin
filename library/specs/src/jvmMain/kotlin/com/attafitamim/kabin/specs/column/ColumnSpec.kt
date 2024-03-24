package com.attafitamim.kabin.specs.column

import com.attafitamim.kabin.annotations.ColumnInfo
import com.attafitamim.kabin.specs.index.PrimaryKeySpec
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

data class ColumnSpec(
    val declaration: KSPropertyDeclaration,
    val name: String,
    val typeAffinity: ColumnInfo.TypeAffinity?,
    val index: Boolean,
    val collate: ColumnInfo.Collate?,
    val defaultValue: String?,
    val primaryKeySpec: PrimaryKeySpec?,
    val typeSpec: ColumnTypeSpec
)
