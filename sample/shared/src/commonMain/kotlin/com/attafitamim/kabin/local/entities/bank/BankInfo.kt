package com.attafitamim.kabin.local.entities.bank

import com.attafitamim.kabin.annotations.Embedded

data class BankInfo(
    val bankNumber: Long,
    val cardToken: String,
    val cardNumber: String,
    @Embedded
    val carPurchase: CarPurchase?,
    val money: Float
)
