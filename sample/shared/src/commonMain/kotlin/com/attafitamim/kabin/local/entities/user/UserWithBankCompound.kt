package com.attafitamim.kabin.local.entities.user

import com.attafitamim.kabin.annotations.Embedded
import com.attafitamim.kabin.annotations.Relation
import com.attafitamim.kabin.local.entities.bank.BankEntity
import com.attafitamim.kabin.local.entities.bank.BankWithCardsCompound
import com.attafitamim.kabin.local.entities.bank.CardEntity

data class UserWithBankCompound(
    @Embedded
    val mainCompound: UserWithSpouseCompound,

    @Relation(
        UserEntity::class,
        parentColumn = "spouseId",
        entityColumn = "id"
    )
    val relationCompound1: UserWithSpouseCompound?,

    @Relation(
        UserEntity::class,
        parentColumn = "spouseId",
        entityColumn = "id"
    )
    val relationCompound2List: List<UserWithSpouseCompound>?,

    @Relation(
        BankEntity::class,
        parentColumn = "embeddedData_bankNumber",
        entityColumn = "number"
    )
    val relationCompound3: BankWithCardsCompound?,

    @Relation(
        CardEntity::class,
        parentColumn = "embeddedData_cardToken",
        entityColumn = "identity_token"
    )
    val relationEntity4: CardEntity?
)
