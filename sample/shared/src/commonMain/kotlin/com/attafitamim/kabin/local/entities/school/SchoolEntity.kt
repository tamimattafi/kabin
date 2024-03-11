package com.attafitamim.kabin.local.entities.school

import com.attafitamim.kabin.annotations.entity.Entity

@Entity
data class SchoolEntity(
    val id: String,
    val name: String,
    val address: String
)
