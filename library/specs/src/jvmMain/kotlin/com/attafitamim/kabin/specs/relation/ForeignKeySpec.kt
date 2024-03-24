package com.attafitamim.kabin.specs.relation

import com.attafitamim.kabin.annotations.ForeignKey
import com.attafitamim.kabin.specs.entity.EntitySpec

data class ForeignKeySpec(
    val entitySpec: EntitySpec,
    val parentColumns: List<String>?,
    val childColumns: List<String>?,
    val onDelete: ForeignKey.Action?,
    val onUpdate: ForeignKey.Action?,
    val deferred: Boolean
)
