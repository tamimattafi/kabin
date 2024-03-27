package com.attafitamim.kabin.local.entities.bank

import com.attafitamim.kabin.annotations.Embedded
import com.attafitamim.kabin.annotations.Relation

data class BankWithCardsCompound(
    @Embedded
    val bankEntity: BankEntity,

    @Relation(
        CardEntity::class,
        parentColumn = "number",
        entityColumn = "identity_bankNumber"
    )
    val cards: List<CardEntity>
)
