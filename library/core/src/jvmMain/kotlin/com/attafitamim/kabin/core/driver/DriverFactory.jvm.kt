package com.attafitamim.kabin.core.driver

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.attafitamim.kabin.core.database.configuration.KabinDatabaseConfiguration
import com.attafitamim.kabin.core.database.configuration.KabinExtendedConfig
import java.util.Properties

private const val FOREIGN_KEY_FLAG = "foreign_keys"

private fun Properties.appendFlags(
    extendedConfig: KabinExtendedConfig
) = apply {
    put(FOREIGN_KEY_FLAG, extendedConfig.foreignKeyConstraintsEnabled)
}

actual fun KabinDatabaseConfiguration.createDriver(
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
): SqlDriver = JdbcSqliteDriver(
    url,
    properties.appendFlags(extendedConfig),
    schema.synchronous(),
    migrateEmptySchema,
    *callbacks
)