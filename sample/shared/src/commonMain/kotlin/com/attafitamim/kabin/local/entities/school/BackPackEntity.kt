package com.attafitamim.kabin.local.entities.school

import com.attafitamim.kabin.annotations.Entity

@Entity
data class BackPackEntity(
    val id: String,
    val studentId: String,
    val weight: String
)
