package com.attafitamim.kabin.local.dao

import com.attafitamim.kabin.annotations.Dao
import com.attafitamim.kabin.annotations.Delete
import com.attafitamim.kabin.annotations.Insert
import com.attafitamim.kabin.annotations.OnConflictStrategy
import com.attafitamim.kabin.annotations.Query
import com.attafitamim.kabin.annotations.Update
import com.attafitamim.kabin.annotations.Upsert
import com.attafitamim.kabin.local.entities.user.UserEntity
import com.attafitamim.kabin.local.entities.user.UserWithBankCompound
import com.attafitamim.kabin.local.entities.user.UserWithSpouseCompound
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
    suspend fun getCompound(age: Int, name: String?): UserWithSpouseCompound?

    @Query("SELECT * FROM UserEntity WHERE sampleAge = :age")
    suspend fun getCompound(age: Int): UserWithSpouseCompound

    @Query("SELECT * FROM UserEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getCompounds(age: Int, name: String?): List<UserWithSpouseCompound>

    @Query("SELECT * FROM UserEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getCompoundReactive(age: Int, name: String?): Flow<UserWithSpouseCompound?>

    @Query("SELECT * FROM UserEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getCompoundsReactive(age: Int, name: String?): Flow<List<UserWithSpouseCompound>>

    @Query("SELECT * FROM UserEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getBankCompound(age: Int, name: String?): UserWithBankCompound

    @Query("SELECT * FROM UserEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getBankCompounds(age: Int, name: String?): List<UserWithBankCompound>

    @Query("SELECT * FROM UserEntity")
    suspend fun getBankCompoundsReactive(): Flow<List<UserWithBankCompound>>?
}
