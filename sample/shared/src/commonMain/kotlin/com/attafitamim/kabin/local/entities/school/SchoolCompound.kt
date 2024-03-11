package com.attafitamim.kabin.local.entities.school

import com.attafitamim.kabin.annotations.entity.Embedded
import com.attafitamim.kabin.annotations.relation.Junction
import com.attafitamim.kabin.annotations.relation.Relation
import com.attafitamim.kabin.local.entities.UserEntity

data class SchoolCompound(
    @Embedded
    val entity: SchoolEntity,

    @Relation(
        entity = StudentEntity::class,
        parentColumn = "identity",
        entityColumn = "id",
        associateBy = Junction(
            SchoolStudentJunction::class,
            parentColumn = "schoolIdentity",
            entityColumn = "studentId"
        )
    )
    val students: List<StudentEntity>,

    @Relation(
        entity = StudentEntity::class,
        parentColumn = "identity",
        entityColumn = "id",
        associateBy = Junction(
            SchoolStudentJunction::class,
            parentColumn = "schoolIdentity",
            entityColumn = "studentId"
        )
    )
    val studentsCompound: List<StudentCompound>
)
