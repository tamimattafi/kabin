package com.attafitamim.kabin.specs.database

import com.attafitamim.kabin.annotations.database.AutoMigration
import com.attafitamim.kabin.specs.core.ClassSpec

data class DatabaseSpec(
    val classSpec: ClassSpec,
    val entities: List<ClassSpec>,
    val views: List<ClassSpec>,
    val version: Int,
    val autoMigrations: List<AutoMigration>
)
