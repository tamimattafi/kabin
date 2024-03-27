package com.attafitamim.kabin.local.entities.bank

import com.attafitamim.kabin.annotations.Embedded
import com.attafitamim.kabin.annotations.Entity

@Entity(primaryKeys = ["identity_token"])
data class CardEntity(
    @Embedded(prefix = "identity_")
    val identity: Identity,
    val holderName: String
) {

    data class Identity(
        val token: String,
        val bankNumber: Long
    )
}
