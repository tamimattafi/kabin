package com.attafitamim.kabin.annotations.database

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Database(
    val entities: Array<KClass<*>>,
    val views: Array<KClass<*>> = [],
    val version: Int,
    val exportScheme: Boolean = true,
    val autoMigrations: Array<AutoMigration> = []
)
