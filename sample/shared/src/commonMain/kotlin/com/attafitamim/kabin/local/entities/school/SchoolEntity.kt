package com.attafitamim.kabin.local.entities.school

import com.attafitamim.kabin.annotations.entity.Entity
import com.attafitamim.kabin.annotations.index.PrimaryKey

@Entity
data class SchoolEntity(
    @PrimaryKey
    val identity: Int,
    val name: String,
    val address: String
)
