package com.attafitamim.kabin.local.entities

import com.attafitamim.kabin.annotations.entity.Embedded
import com.attafitamim.kabin.annotations.relation.Relation

data class BankWithCardsCompound(
    @Embedded
    val bankEntity: BankEntity,

    @Relation(
        CardEntity::class,
        parentColumn = "number",
        entityColumn = "bankNumber"
    )
    val cards: List<CardEntity>
)
