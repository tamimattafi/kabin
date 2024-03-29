package com.attafitamim.kabin.local.entities.user

import com.attafitamim.kabin.annotations.ColumnInfo
import com.attafitamim.kabin.annotations.Ignore
import com.attafitamim.kabin.annotations.Embedded
import com.attafitamim.kabin.annotations.Entity
import com.attafitamim.kabin.annotations.Index
import com.attafitamim.kabin.annotations.PrimaryKey
import com.attafitamim.kabin.local.entities.bank.BankInfo

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
