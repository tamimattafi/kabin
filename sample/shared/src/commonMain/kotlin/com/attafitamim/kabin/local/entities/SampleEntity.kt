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
    ignoredColumns = ["ignoredInfo"]
)
data class SampleEntity(
    @PrimaryKey
    val id: String,
    @PrimaryKey(autoGenerate = true)
    val secondId: Int,
    val name: String,
    @ColumnInfo("sampleAge")
    val age: Int,
    @Ignore
    val ignoredInfo: String
)
