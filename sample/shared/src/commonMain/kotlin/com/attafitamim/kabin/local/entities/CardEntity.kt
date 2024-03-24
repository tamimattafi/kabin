package com.attafitamim.kabin.local.entities

import com.attafitamim.kabin.annotations.Embedded
import com.attafitamim.kabin.annotations.Entity
import com.attafitamim.kabin.annotations.PrimaryKey

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
