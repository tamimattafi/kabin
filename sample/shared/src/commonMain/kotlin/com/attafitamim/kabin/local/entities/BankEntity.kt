package com.attafitamim.kabin.local.entities

import com.attafitamim.kabin.annotations.Entity
import com.attafitamim.kabin.annotations.PrimaryKey

@Entity
data class BankEntity(
    @PrimaryKey
    val number: Long,
    val country: String,
    val region: String,
    val supportedCards: List<String>
)
