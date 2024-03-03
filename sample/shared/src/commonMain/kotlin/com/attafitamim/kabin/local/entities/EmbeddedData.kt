package com.attafitamim.kabin.local.entities

import com.attafitamim.kabin.annotations.entity.Embedded

data class EmbeddedData(
    val bankNumber: Long,
    val cardNumber: String,
    @Embedded
    val otherEmbeddedData: OtherEmbeddedData,
    val money: Float
)
