package com.attafitamim.kabin.local.entities.school

import com.attafitamim.kabin.annotations.Embedded
import com.attafitamim.kabin.annotations.Junction
import com.attafitamim.kabin.annotations.Relation

data class SchoolCompound(
    @Embedded
    val entity: SchoolEntity,

    @Relation(
        entity = StudentEntity::class,
        parentColumn = "name",
        entityColumn = "fullName"
    )
    val luckyStudent: StudentCompound?,

    @Relation(
        entity = StudentEntity::class,
        parentColumn = "address",
        entityColumn = "id"
    )
    val otherLuckyStudent: StudentCompound,

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
