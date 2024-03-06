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
    val relationCompound1: UserWithSpouseCompound,

    @Relation(
        UserEntity::class,
        parentColumn = "spouseId",
        entityColumn = "id"
    )
    val relationCompound2List: List<UserWithSpouseCompound>,

    @Relation(
        BankEntity::class,
        parentColumn = "embeddedData_bankNumber",
        entityColumn = "number"
    )
    val relationCompound3: BankWithCardsCompound,

    @Relation(
        CardEntity::class,
        parentColumn = "embeddedData_cardToken",
        entityColumn = "token"
    )
    val relationEntity4: CardEntity
)
