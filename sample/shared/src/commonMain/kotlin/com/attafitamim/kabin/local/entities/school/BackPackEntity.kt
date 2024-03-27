package com.attafitamim.kabin.local.entities.school

import com.attafitamim.kabin.annotations.Entity
import com.attafitamim.kabin.annotations.PrimaryKey

@Entity
data class BackPackEntity(
    @PrimaryKey
    val id: String,
    val studentId: String,
    val weight: String
)
