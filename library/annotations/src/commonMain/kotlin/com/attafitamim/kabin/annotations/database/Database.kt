package com.attafitamim.kabin.annotations.database

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Database(
    val entities: Array<KClass<*>>,
    val views: Array<KClass<*>> = [],
    val version: Int,
    val exportScheme: Boolean = DEFAULT_EXPORT_SCHEME,
    val autoMigrations: Array<AutoMigration> = []
) {

    companion object {
        const val DEFAULT_EXPORT_SCHEME = true
    }
}
