package com.attafitamim.kabin.local.entities

import com.attafitamim.kabin.annotations.entity.Entity
import com.attafitamim.kabin.annotations.index.PrimaryKey
import com.attafitamim.kabin.annotations.relation.ForeignKey

@Entity(
    foreignKeys = [
        ForeignKey(
            UserEntity::class,
            parentColumns = ["embeddedData_bankNumber"],
            childColumns = ["number"],
            onDelete = ForeignKey.Action.CASCADE,
            onUpdate = ForeignKey.Action.CASCADE,
            deferred = false
        )
    ]
)
data class BankEntity(
    @PrimaryKey
    val number: Long,
    val country: String,
    val region: String
)
