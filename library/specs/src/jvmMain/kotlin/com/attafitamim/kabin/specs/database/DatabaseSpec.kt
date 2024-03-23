package com.attafitamim.kabin.specs.database

import com.attafitamim.kabin.annotations.AutoMigration
import com.attafitamim.kabin.specs.converters.MapperSpec
import com.attafitamim.kabin.specs.converters.TypeConverterSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.symbol.KSClassDeclaration

data class DatabaseSpec(
    val declaration: KSClassDeclaration,
    val entities: List<EntitySpec>,
    val views: List<KSClassDeclaration>?,
    val version: Int,
    val exportScheme: Boolean,
    val autoMigrations: List<AutoMigration>?,
    val daoGetters: List<DatabaseDaoGetterSpec>,
    val typeConverters: List<TypeConverterSpec>?,
    val mappers: List<MapperSpec>?
)
