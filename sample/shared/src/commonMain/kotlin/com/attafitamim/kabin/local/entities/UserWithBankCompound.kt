package com.attafitamim.kabin.local.entities

import com.attafitamim.kabin.annotations.entity.Embedded
import com.attafitamim.kabin.annotations.relation.Relation

data class UserWithBankCompound(
    @Embedded
    val userWithSpouseCompound: UserWithSpouseCompound,

    @Relation(
        BankEntity::class,
        parentColumn = "bankNumber",
        entityColumn = "number"
    )
    val bankEntity: BankEntity
)