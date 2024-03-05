package com.attafitamim.kabin.local.entities

import com.attafitamim.kabin.annotations.entity.Embedded
import com.attafitamim.kabin.annotations.relation.Relation

data class UserWithBankCompound(
    @Embedded
    val mainCompound: UserWithSpouseCompound,

    @Relation(
        UserEntity::class,
        parentColumn = "spouseId",
        entityColumn = "id"
    )
    val relationCompound1: UserWithSpouseCompound
)
