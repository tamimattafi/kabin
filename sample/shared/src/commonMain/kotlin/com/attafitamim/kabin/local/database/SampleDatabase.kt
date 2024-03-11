package com.attafitamim.kabin.local.database

import com.attafitamim.kabin.annotations.converters.Mappers
import com.attafitamim.kabin.annotations.converters.TypeConverters
import com.attafitamim.kabin.annotations.database.Database
import com.attafitamim.kabin.core.database.KabinDatabase
import com.attafitamim.kabin.local.converter.IntStringAdapter
import com.attafitamim.kabin.local.converter.ListOfStringByteArrayAdapter
import com.attafitamim.kabin.local.converter.MarriedCountMapper
import com.attafitamim.kabin.local.dao.UserCompoundsDao
import com.attafitamim.kabin.local.dao.UserDao
import com.attafitamim.kabin.local.entities.BankEntity
import com.attafitamim.kabin.local.entities.CardEntity
import com.attafitamim.kabin.local.entities.UserEntity

@Database(
    entities = [
        UserEntity::class,
        BankEntity::class,
        CardEntity::class
    ],
    version = 8
)
@TypeConverters(
    IntStringAdapter::class,
    ListOfStringByteArrayAdapter::class
)
@Mappers(MarriedCountMapper::class)
interface SampleDatabase : KabinDatabase {
    val userDao: UserDao
    val userCompoundsDao: UserCompoundsDao

    companion object {
        const val NAME = "sample-database"
    }
}
