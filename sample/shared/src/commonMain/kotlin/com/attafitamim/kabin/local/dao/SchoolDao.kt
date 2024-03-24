package com.attafitamim.kabin.local.dao

import com.attafitamim.kabin.annotations.Dao
import com.attafitamim.kabin.annotations.Insert
import com.attafitamim.kabin.annotations.OnConflictStrategy
import com.attafitamim.kabin.annotations.Query
import com.attafitamim.kabin.local.entities.school.BackPackEntity
import com.attafitamim.kabin.local.entities.school.SchoolCompound
import com.attafitamim.kabin.local.entities.school.StudentCompound
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(schoolCompound: SchoolCompound)

    @Query("SELECT * FROM SchoolEntity WHERE id = :id")
    suspend fun getSchoolCompound(id: String): SchoolCompound

    @Query("SELECT * FROM SchoolEntity WHERE id = :id")
    suspend fun getStudentCompound(id: String): StudentCompound

    @Query("SELECT * FROM BackPackEntity WHERE(:studentIds IS NULL OR studentId IN :studentIds)")
    suspend fun getStudentBackPacks(
        studentIds: List<String>?
    ): Flow<List<BackPackEntity>>
}
