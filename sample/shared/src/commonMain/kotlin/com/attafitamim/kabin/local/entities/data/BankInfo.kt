package com.attafitamim.kabin.local.entities.data

import com.attafitamim.kabin.annotations.entity.Embedded

data class BankInfo(
    val bankNumber: Long,
    val cardToken: String,
    val cardNumber: String,
    @Embedded
    val carPurchase: CarPurchase?,
    val money: Float
)
