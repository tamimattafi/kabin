package com.attafitamim.kabin.specs.relation

import com.attafitamim.kabin.specs.entity.EntitySpec

data class JunctionSpec(
    val entitySpec: EntitySpec,
    val parentColumn: String,
    val entityColumn: String
)
