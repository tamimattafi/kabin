package com.attafitamim.kabin.local.database

import com.attafitamim.kabin.annotations.converters.Mappers
import com.attafitamim.kabin.annotations.converters.TypeConverters
import com.attafitamim.kabin.annotations.database.Database
import com.attafitamim.kabin.core.database.KabinDatabase
import com.attafitamim.kabin.local.converter.IntStringAdapter
import com.attafitamim.kabin.local.converter.ListOfStringByteArrayAdapter
import com.attafitamim.kabin.local.converter.MarriedCountMapper
import com.attafitamim.kabin.local.dao.SchoolDao
import com.attafitamim.kabin.local.dao.UserCompoundsDao
import com.attafitamim.kabin.local.dao.UserDao
import com.attafitamim.kabin.local.entities.BankEntity
import com.attafitamim.kabin.local.entities.CardEntity
import com.attafitamim.kabin.local.entities.UserEntity
import com.attafitamim.kabin.local.entities.UserSearchEntity
import com.attafitamim.kabin.local.entities.school.BackPackEntity
import com.attafitamim.kabin.local.entities.school.SchoolEntity
import com.attafitamim.kabin.local.entities.school.SchoolStudentJunction
import com.attafitamim.kabin.local.entities.school.StudentEntity

@Database(
    entities = [
        UserEntity::class,
        UserSearchEntity::class,
        BankEntity::class,
        CardEntity::class,
        SchoolEntity::class,
        StudentEntity::class,
        SchoolStudentJunction::class,
        BackPackEntity::class
    ],
    version = 9
)
@TypeConverters(
    IntStringAdapter::class,
    ListOfStringByteArrayAdapter::class
)
@Mappers(MarriedCountMapper::class)
interface SampleDatabase : KabinDatabase {
    val userDao: UserDao
    val userCompoundsDao: UserCompoundsDao
    val schoolDao: SchoolDao

    companion object {
        const val NAME = "sample-database"
    }
}
