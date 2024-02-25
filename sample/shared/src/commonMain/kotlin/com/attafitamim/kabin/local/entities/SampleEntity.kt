package com.attafitamim.kabin.local.entities

import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.annotations.column.Ignore
import com.attafitamim.kabin.annotations.entity.Entity
import com.attafitamim.kabin.annotations.relation.ForeignKey
import com.attafitamim.kabin.annotations.index.Index
import com.attafitamim.kabin.annotations.index.PrimaryKey

@Entity(
    indices = [
        Index(
            "sampleAge",
            orders = [Index.Order.ASC],
            unique = true
        )
    ],
    primaryKeys = [
        "id",
        "phoneNumber"
    ],
    foreignKeys = [
        ForeignKey(
            SampleEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.Action.CASCADE,
            onUpdate = ForeignKey.Action.SET_DEFAULT,
            deferred = true
        )
    ],
    ignoredColumns = ["secret"]
)
data class SampleEntity(
    @PrimaryKey
    val id: String,
    @PrimaryKey
    val phoneNumber: Int,
    @ColumnInfo(defaultValue = "James")
    val name: String,
    @ColumnInfo("sampleAge", ColumnInfo.TypeAffinity.TEXT)
    val age: Int,
    val salary: Double?,
    @Ignore
    val secret: String
)
