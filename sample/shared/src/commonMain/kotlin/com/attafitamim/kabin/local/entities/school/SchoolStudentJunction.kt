package com.attafitamim.kabin.local.entities.school

import com.attafitamim.kabin.annotations.entity.Entity

@Entity(primaryKeys = ["schoolIdentity", "studentId"])
data class SchoolStudentJunction(
    val schoolIdentity: Int,
    val studentId: String
)