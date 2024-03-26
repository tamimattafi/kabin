package com.attafitamim.kabin.core.driver

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.attafitamim.kabin.core.database.KabinDatabaseConfiguration
import java.util.Properties

private const val FOREIGN_KEY_FLAG = "foreign_keys"

private fun Properties.appendFlags(
    foreignKeyConstraintsEnabled: Boolean
) = apply {
    put(FOREIGN_KEY_FLAG, foreignKeyConstraintsEnabled)
}

actual fun KabinDatabaseConfiguration.createDriver(
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
): SqlDriver = JdbcSqliteDriver(
    url,
    properties.appendFlags(foreignKeyConstraintsEnabled),
    schema.synchronous(),
    migrateEmptySchema,
    *callbacks
)