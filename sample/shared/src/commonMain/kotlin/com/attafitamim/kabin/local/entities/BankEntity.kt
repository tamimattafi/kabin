package com.attafitamim.kabin.local.entities

import com.attafitamim.kabin.annotations.entity.Entity
import com.attafitamim.kabin.annotations.index.PrimaryKey

@Entity
data class BankEntity(
    @PrimaryKey
    val number: Long,
    val country: String,
    val region: String
)
