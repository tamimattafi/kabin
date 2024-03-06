package com.attafitamim.kabin.local.entities

import com.attafitamim.kabin.annotations.entity.Embedded

data class BankInfo(
    val bankNumber: Long,
    val cardToken: String,
    val cardNumber: String,
    @Embedded
    val carPurchase: CarPurchase,
    val money: Float
)
