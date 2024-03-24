package com.attafitamim.kabin.local.entities.school

import com.attafitamim.kabin.annotations.Entity
import com.attafitamim.kabin.annotations.PrimaryKey

@Entity
data class StudentEntity(
    @PrimaryKey
    val id: String,
    val fullName: String,
    val grade: Int,
    val schoolId: Int
)
