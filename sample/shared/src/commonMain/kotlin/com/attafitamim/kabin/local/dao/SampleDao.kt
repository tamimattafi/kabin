package com.attafitamim.kabin.local.dao

import com.attafitamim.kabin.annotations.dao.Dao
import com.attafitamim.kabin.annotations.dao.Delete
import com.attafitamim.kabin.annotations.dao.Insert
import com.attafitamim.kabin.annotations.dao.OnConflictStrategy
import com.attafitamim.kabin.annotations.dao.Query
import com.attafitamim.kabin.annotations.dao.RawQuery
import com.attafitamim.kabin.annotations.dao.Transaction
import com.attafitamim.kabin.annotations.dao.Update
import com.attafitamim.kabin.local.entities.SampleEntity

@Dao
interface SampleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: SampleEntity)

    @Update
    suspend fun someUpdate(entity: SampleEntity)

    @Delete
    @Transaction
    suspend fun delete(entity: SampleEntity)

    @Query("SELECT * FROM SampleEntity WHERE name = :name AND age = :age")
    suspend fun getEntity(age: Int, name: String): SampleEntity

    @Query("SELECT name FROM SampleEntity LIMIT 1")
    suspend fun getSomething(): String

    @RawQuery
    @Transaction
    suspend fun getEntities(query: String): List<SampleEntity>

    @Transaction
    suspend fun updateReplacing(entity: SampleEntity) {
        delete(entity)
        insertOrUpdate(entity)
    }
}
