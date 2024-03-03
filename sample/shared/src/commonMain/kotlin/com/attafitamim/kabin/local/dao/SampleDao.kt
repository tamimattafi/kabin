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
import com.attafitamim.kabin.local.entities.SampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SampleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(entity: SampleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceTwo(entity: SampleEntity, entity2: SampleEntity?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceList(entities: List<SampleEntity>?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceMixed(entities: List<SampleEntity>?, entity2: SampleEntity)

    @Update
    suspend fun update(entity: SampleEntity)

    @Delete
    @Transaction
    suspend fun delete(entity: SampleEntity)

    @Query("SELECT * FROM SampleEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getEntity(age: Int, name: String?): SampleEntity

    @Query("SELECT * FROM SampleEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getEntityOrNull(age: Int, name: String): SampleEntity?

    @Query("SELECT * FROM SampleEntity WHERE name = :name AND sampleAge = :age")
    suspend fun getEntityReactive(age: Int, name: String): Flow<SampleEntity?>

    @Query("SELECT name FROM SampleEntity LIMIT 1")
    suspend fun getName(): String

    @Query("SELECT EXISTS(SELECT 1 FROM SampleEntity WHERE id = :id )")
    suspend fun hasEntry(id: String): Boolean

    @Query("SELECT COUNT(id) FROM SampleEntity WHERE isMarried = 1")
    suspend fun getMarriedCount(): MarriedCount

    @RawQuery
    @Transaction
    suspend fun getEntities(query: String): Iterable<SampleEntity>

    @Transaction
    suspend fun updateReplacing(entity: SampleEntity) {
        delete(entity)
        insertOrReplace(entity)
    }
}
