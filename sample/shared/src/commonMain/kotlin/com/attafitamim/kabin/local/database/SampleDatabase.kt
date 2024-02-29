package com.attafitamim.kabin.local.database

import com.attafitamim.kabin.annotations.converters.TypeConverters
import com.attafitamim.kabin.annotations.database.Database
import com.attafitamim.kabin.core.database.KabinDatabase
import com.attafitamim.kabin.local.converter.IntStringConverter
import com.attafitamim.kabin.local.dao.SampleDao
import com.attafitamim.kabin.local.entities.SampleEntity

@Database(
    entities = [SampleEntity::class],
    version = 2
)
@TypeConverters(
    IntStringConverter::class
)
interface SampleDatabase : KabinDatabase {
    val dao: SampleDao
}
