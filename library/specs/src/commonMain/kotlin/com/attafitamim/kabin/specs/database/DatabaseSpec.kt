package com.attafitamim.kabin.specs.database

import com.attafitamim.kabin.annotations.database.AutoMigration
import com.attafitamim.kabin.core.database.KabinDatabase
import com.attafitamim.kabin.specs.entity.EntitySpec
import kotlin.reflect.KClass

data class DatabaseSpec(
    val clazz: KClass<out KabinDatabase>,
    val entities: List<EntitySpec>,
    val views: List<EntitySpec>,
    val version: Int,
    val autoMigrations: List<AutoMigration>
)
