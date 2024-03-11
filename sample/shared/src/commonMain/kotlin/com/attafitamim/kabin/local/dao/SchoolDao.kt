package com.attafitamim.kabin.local.dao

import com.attafitamim.kabin.annotations.dao.Dao
import com.attafitamim.kabin.annotations.dao.Insert
import com.attafitamim.kabin.annotations.dao.OnConflictStrategy
import com.attafitamim.kabin.annotations.dao.Query
import com.attafitamim.kabin.local.entities.school.SchoolCompound
import com.attafitamim.kabin.local.entities.school.StudentCompound

@Dao
interface SchoolDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(schoolCompound: SchoolCompound)

    @Query("SELECT * FROM SchoolEntity WHERE id = :id")
    suspend fun getSchoolCompound(id: String): SchoolCompound

    @Query("SELECT * FROM SchoolEntity WHERE id = :id")
    suspend fun getStudentCompound(id: String): StudentCompound
}