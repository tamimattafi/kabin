package com.attafitamim.kabin.local.entities.school

import com.attafitamim.kabin.annotations.entity.Entity
import com.attafitamim.kabin.annotations.index.PrimaryKey

@Entity
data class StudentEntity(
    @PrimaryKey
    val id: String,
    val fullName: String,
    val grade: Int,
    val schoolId: Int
)
