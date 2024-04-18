package com.attafitamim.kabin.local.entities.school

import com.attafitamim.kabin.annotations.Entity
import com.attafitamim.kabin.annotations.ForeignKey
import com.attafitamim.kabin.annotations.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            SchoolEntity::class,
            parentColumns = ["identity"],
            childColumns = ["schoolId"],
            onDelete = ForeignKey.Action.CASCADE,
            onUpdate = ForeignKey.Action.CASCADE
        )
    ]
)
data class StudentEntity(
    @PrimaryKey
    val id: String,
    val fullName: String,
    val grade: Int,
    val schoolId: Int
)
