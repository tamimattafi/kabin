package com.attafitamim.kabin.local.database

import com.attafitamim.kabin.annotations.converters.Mappers
import com.attafitamim.kabin.annotations.converters.TypeConverters
import com.attafitamim.kabin.annotations.database.Database
import com.attafitamim.kabin.core.database.KabinDatabase
import com.attafitamim.kabin.local.converter.IntStringConverter
import com.attafitamim.kabin.local.converter.MarriedCountMapper
import com.attafitamim.kabin.local.dao.UserDao
import com.attafitamim.kabin.local.entities.UserEntity

@Database(
    entities = [UserEntity::class],
    version = 6
)
@TypeConverters(IntStringConverter::class)
@Mappers(MarriedCountMapper::class)
interface SampleDatabase : KabinDatabase {
    val userDao: UserDao
}
