package com.attafitamim.kabin.local.entities.school

import com.attafitamim.kabin.annotations.entity.Entity

@Entity
data class StudentEntity(
    val id: String,
    val fullName: String,
    val grade: Int
)
