package com.attafitamim.kabin.local.database

import com.attafitamim.kabin.annotations.Mappers
import com.attafitamim.kabin.annotations.TypeConverters
import com.attafitamim.kabin.annotations.Database
import com.attafitamim.kabin.core.database.KabinDatabase
import com.attafitamim.kabin.local.converter.IntStringAdapter
import com.attafitamim.kabin.local.converter.ListOfStringByteArrayAdapter
import com.attafitamim.kabin.local.converter.MarriedCountMapper
import com.attafitamim.kabin.local.dao.SchoolDao
import com.attafitamim.kabin.local.dao.UserCompoundsDao
import com.attafitamim.kabin.local.dao.UserDao
import com.attafitamim.kabin.local.entities.bank.BankEntity
import com.attafitamim.kabin.local.entities.bank.CardEntity
import com.attafitamim.kabin.local.entities.user.UserEntity
import com.attafitamim.kabin.local.entities.user.UserSearchEntity
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
    version = 26
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
}
