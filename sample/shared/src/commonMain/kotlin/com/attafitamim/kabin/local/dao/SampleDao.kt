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
    fun insertOrUpdate(entity: SampleEntity)

    @Update(onConflict = OnConflictStrategy.IGNORE)
    fun update(entity: SampleEntity)

    @Delete
    fun delete(entity: SampleEntity)

    @Query("SELECT * FROM SampleEntity WHERE name = :name AND age = :age")
    fun getEntity(name: String, age: Int): SampleEntity

    @RawQuery
    fun getEntities(query: String): List<SampleEntity>

    @Transaction
    fun updateReplacing(entity: SampleEntity) {
        delete(entity)
        insertOrUpdate(entity)
    }
}
