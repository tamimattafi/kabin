package com.attafitamim.kabin.specs.relation

import com.attafitamim.kabin.annotations.relation.ForeignKey
import com.google.devtools.ksp.symbol.KSType

data class ForeignKeySpec(
    val entityType: KSType,
    val parentColumns: List<String>?,
    val childColumns: List<String>?,
    val onDelete: ForeignKey.Action?,
    val onUpdate: ForeignKey.Action?,
    val deferred: Boolean
)
