package com.attafitamim.kabin.local.entities

import com.attafitamim.kabin.annotations.entity.Entity
import com.attafitamim.kabin.annotations.index.PrimaryKey

@Entity
data class SampleEntity(
    @PrimaryKey
    val id: String,
    val name: String
)
