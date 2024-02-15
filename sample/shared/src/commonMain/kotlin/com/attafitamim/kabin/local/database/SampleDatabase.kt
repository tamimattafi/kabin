package com.attafitamim.kabin.local.database

import com.attafitamim.kabin.annotations.database.Database
import com.attafitamim.kabin.core.database.KabinDatabase
import com.attafitamim.kabin.local.entities.SampleEntity

@Database(
    entities = [SampleEntity::class],
    version = 2
)
abstract class SampleDatabase : KabinDatabase()