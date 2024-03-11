package com.attafitamim.kabin.local.entities.school

import com.attafitamim.kabin.annotations.entity.Entity

@Entity
data class SchoolStudentJunction(
    val schoolId: String,
    val studentId: String
)