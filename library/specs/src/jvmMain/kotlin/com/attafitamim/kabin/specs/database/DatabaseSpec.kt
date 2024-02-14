package com.attafitamim.kabin.specs.database

import com.attafitamim.kabin.annotations.database.AutoMigration
import com.google.devtools.ksp.symbol.KSClassDeclaration

data class DatabaseSpec(
    val declaration: KSClassDeclaration,
    val entities: List<KSClassDeclaration>,
    val views: List<KSClassDeclaration>?,
    val version: Int,
    val exportScheme: Boolean,
    val autoMigrations: List<AutoMigration>?
)
