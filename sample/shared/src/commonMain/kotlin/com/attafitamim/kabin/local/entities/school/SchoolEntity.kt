package com.attafitamim.kabin.local.entities.school

import com.attafitamim.kabin.annotations.Entity
import com.attafitamim.kabin.annotations.PrimaryKey

@Entity
data class SchoolEntity(
    @PrimaryKey
    val identity: Int,
    val name: String,
    val address: String
)
