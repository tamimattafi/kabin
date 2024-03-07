package com.attafitamim.kabin.local.dao

import com.attafitamim.kabin.annotations.dao.Dao
import com.attafitamim.kabin.annotations.dao.Delete
import com.attafitamim.kabin.annotations.dao.Insert
import com.attafitamim.kabin.annotations.dao.OnConflictStrategy
import com.attafitamim.kabin.annotations.dao.Query
import com.attafitamim.kabin.annotations.dao.Transaction
import com.attafitamim.kabin.annotations.dao.Update
import com.attafitamim.kabin.annotations.dao.Upsert
import com.attafitamim.kabin.local.entities.UserEntity
import com.attafitamim.kabin.local.entities.UserWithBankCompound
import com.attafitamim.kabin.local.entities.UserWithSpouseCompound
import kotlinx.coroutines.flow.Flow

@Dao
interface UserCompoundsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: UserWithSpouseCompound)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: UserWithBankCompound)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(entity: UserWithBankCompound)

    @Delete
    suspend fun delete(entity: UserWithBankCompound)

    @Upsert
    suspend fun upsert(entity: UserWithBankCompound)

    @Query("SELECT * FROM UserEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getCompound(age: Int, name: String?): UserWithSpouseCompound

    @Query("SELECT * FROM UserEntity WHERE sampleAge = :age")
    suspend fun getCompound(age: Int): UserWithSpouseCompound

    @Query("SELECT * FROM UserEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getEntity(age: Int, name: String?): UserEntity

    @Query("SELECT * FROM UserEntity WHERE sampleAge = :age")
    suspend fun getEntity(age: Int): UserEntity

    @Query("SELECT * FROM UserEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getCompounds(age: Int, name: String?): List<UserWithSpouseCompound>

    @Query("SELECT * FROM UserEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getCompoundReactive(age: Int, name: String?): Flow<UserWithSpouseCompound?>

    @Query("SELECT * FROM UserEntity WHERE name = :name AND sampleAge = :age")
    @Transaction
    suspend fun getCompoundsReactive(age: Int, name: String?): Flow<List<UserWithSpouseCompound>>

    @Query("SELECT * FROM UserEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getBankCompound(age: Int, name: String?): UserWithBankCompound

    @Query("SELECT * FROM UserEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getBankCompounds(age: Int, name: String?): List<UserWithBankCompound>

    @Query("SELECT * FROM UserEntity")
    suspend fun getBankCompoundsReactive(): Flow<List<UserWithBankCompound>>
}
