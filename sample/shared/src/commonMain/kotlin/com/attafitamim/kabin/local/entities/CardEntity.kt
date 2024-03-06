package com.attafitamim.kabin.local.entities

import com.attafitamim.kabin.annotations.entity.Embedded
import com.attafitamim.kabin.annotations.entity.Entity
import com.attafitamim.kabin.annotations.index.PrimaryKey

@Entity
data class CardEntity(
    @PrimaryKey
    @Embedded
    val identity: Identity,
    val holderName: String
) {

    data class Identity(
        val token: String,
        val bankNumber: Long
    )
}
