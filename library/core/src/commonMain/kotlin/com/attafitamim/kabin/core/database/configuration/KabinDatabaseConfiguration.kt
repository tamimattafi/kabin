package com.attafitamim.kabin.core.database.configuration

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.attafitamim.kabin.core.utils.toggleForeignKeys

expect class KabinDatabaseConfiguration {
    val extendedConfig: KabinExtendedConfig

    fun createSqlDriver(
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
    ): SqlDriver
}

fun SqlDriver.configure(configuration: KabinDatabaseConfiguration) = apply {
    toggleForeignKeys(configuration.extendedConfig.foreignKeyConstraintsEnabled)
}
