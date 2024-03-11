package com.attafitamim.kabin.local.entities.school

import com.attafitamim.kabin.annotations.entity.Embedded
import com.attafitamim.kabin.annotations.relation.Relation

data class StudentCompound(
    @Embedded
    val student: StudentEntity,

    @Relation(
        BackPackEntity::class,
        parentColumn = "id",
        entityColumn = "studentId"
    )
    val backPack: BackPackEntity
)
