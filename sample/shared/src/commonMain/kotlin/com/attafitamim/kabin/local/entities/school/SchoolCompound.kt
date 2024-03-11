package com.attafitamim.kabin.local.entities.school

import com.attafitamim.kabin.annotations.entity.Embedded
import com.attafitamim.kabin.annotations.relation.Junction
import com.attafitamim.kabin.annotations.relation.Relation

data class SchoolCompound(
    @Embedded
    val entity: SchoolEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            SchoolStudentJunction::class,
            parentColumn = "schoolId",
            entityColumn = "studentId"
        )
    )
    val students: List<StudentEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            SchoolStudentJunction::class,
            parentColumn = "schoolId",
            entityColumn = "studentId"
        )
    )
    val studentsCompound: List<StudentCompound>
)
