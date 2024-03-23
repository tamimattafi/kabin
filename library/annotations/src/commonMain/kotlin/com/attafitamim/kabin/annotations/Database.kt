package com.attafitamim.kabin.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Database(
    val entities: Array<KClass<*>>,
    val views: Array<KClass<*>> = [],
    val version: Int,
    val exportSchema: Boolean = DEFAULT_EXPORT_SCHEMA,
    val autoMigrations: Array<AutoMigration> = []
) {

    companion object {
        const val DEFAULT_EXPORT_SCHEMA = false
    }
}
