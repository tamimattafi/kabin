package com.attafitamim.kabin.local.entities

import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.annotations.column.Ignore
import com.attafitamim.kabin.annotations.entity.Embedded
import com.attafitamim.kabin.annotations.entity.Entity
import com.attafitamim.kabin.annotations.index.Index
import com.attafitamim.kabin.annotations.index.PrimaryKey
import com.attafitamim.kabin.local.entities.data.BankInfo
import com.attafitamim.kabin.local.entities.data.Gender

@Entity(
    indices = [
        Index(
            "sampleAge",
            orders = [Index.Order.ASC],
            unique = true
        ),
        Index(
            "name",
            orders = [Index.Order.ASC],
            unique = true
        ),
        Index(
            "salary",
            orders = [Index.Order.ASC],
            unique = true
        )
    ],
    primaryKeys = [
        "id",
        "phoneNumber"
    ],
    ignoredColumns = ["secret"]
)
data class UserEntity(
    @PrimaryKey
    val id: Int,
    @PrimaryKey
    val phoneNumber: String,
    val gender: Gender,
    @ColumnInfo(defaultValue = "James")
    val name: String,
    @ColumnInfo("sampleAge", ColumnInfo.TypeAffinity.TEXT)
    val age: Int,
    val salary: Float?,
    val isMarried: Boolean,
    val spouseId: Int?,
    val data: String,
    @Embedded(prefix = "embeddedData_")
    val bankInfo: BankInfo?,
    @Ignore
    val secret: String = ""
)
