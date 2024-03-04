package com.attafitamim.kabin.local.dao

import com.attafitamim.kabin.annotations.dao.Dao
import com.attafitamim.kabin.annotations.dao.Delete
import com.attafitamim.kabin.annotations.dao.Insert
import com.attafitamim.kabin.annotations.dao.OnConflictStrategy
import com.attafitamim.kabin.annotations.dao.Query
import com.attafitamim.kabin.annotations.dao.RawQuery
import com.attafitamim.kabin.annotations.dao.Transaction
import com.attafitamim.kabin.annotations.dao.Update
import com.attafitamim.kabin.local.entities.MarriedCount
import com.attafitamim.kabin.local.entities.UserEntity
import com.attafitamim.kabin.local.entities.UserWithSpouseCompound
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: UserEntity?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: UserWithSpouseCompound)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceTwo(entity: UserEntity, entity2: UserEntity?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceList(entities: List<UserEntity>?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceMixed(entities: List<UserEntity>?, entity2: UserEntity)

    @Update
    suspend fun update(entity: UserEntity)

    @Delete
    @Transaction
    suspend fun delete(entity: UserEntity)

    @Query("SELECT * FROM UserEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getEntity(age: Int, name: String?): UserEntity

    @Query("SELECT * FROM UserEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getCompound(age: Int, name: String?): UserWithSpouseCompound

    @Query("SELECT * FROM UserEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getCompounds(age: Int, name: String?): List<UserWithSpouseCompound>

    @Query("SELECT * FROM UserEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getCompoundReactive(age: Int, name: String?): Flow<UserWithSpouseCompound>

    @Query("SELECT * FROM UserEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getCompoundsReactive(age: Int, name: String?): Flow<List<UserWithSpouseCompound>>

    @Query("SELECT * FROM UserEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getEntityOrNull(age: Int, name: String): UserEntity?

    @Query("SELECT * FROM UserEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getEntityReactive(age: Int, name: String): Flow<UserEntity?>

    @Query("SELECT * FROM UserEntity")
    suspend fun getEntitiesReactive(): Flow<List<UserEntity>>

    @Query("SELECT name FROM UserEntity LIMIT 1")
    suspend fun getName(): String

    @Query("SELECT EXISTS(SELECT 1 FROM UserEntity WHERE id = :id )")
    suspend fun hasEntry(id: String): Boolean

    @Query("SELECT COUNT(id) FROM UserEntity WHERE isMarried = 1")
    suspend fun getMarriedCount(): MarriedCount

    @Query("DROP TABLE IF EXISTS UserEntity")
    suspend fun drop()

    @RawQuery
    @Transaction
    suspend fun getEntities(query: String): Iterable<UserEntity>

    @Transaction
    suspend fun updateReplacing(entity: UserEntity) {
        delete(entity)
        insertOrReplace(entity)
    }
}
