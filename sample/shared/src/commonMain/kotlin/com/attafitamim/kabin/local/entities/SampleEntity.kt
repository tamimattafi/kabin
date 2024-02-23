package com.attafitamim.kabin.local.entities

import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.annotations.column.Ignore
import com.attafitamim.kabin.annotations.entity.Entity
import com.attafitamim.kabin.annotations.relation.ForeignKey
import com.attafitamim.kabin.annotations.index.Index
import com.attafitamim.kabin.annotations.index.PrimaryKey

@Entity(
    indices = [
        Index("age", unique = false)
    ],
    primaryKeys = [
        "id", "secondId"
    ],
    foreignKeys = [
        ForeignKey(
            SampleEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"]
        )
    ],
    ignoredColumns = ["secret"]
)
data class SampleEntity(
    @PrimaryKey
    val id: String,
    @PrimaryKey(autoGenerate = true)
    val phoneNumber: Int,
    @ColumnInfo(defaultValue = "James")
    val name: String,
    @ColumnInfo("sampleAge", ColumnInfo.TypeAffinity.TEXT)
    val age: Int,
    val salary: Double?,
    @Ignore
    val secret: String
)
