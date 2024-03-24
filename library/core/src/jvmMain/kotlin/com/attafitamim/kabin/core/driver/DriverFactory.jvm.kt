package com.attafitamim.kabin.core.driver

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.attafitamim.kabin.core.database.KabinDatabaseConfiguration

actual fun KabinDatabaseConfiguration.createDriver(
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
): SqlDriver = JdbcSqliteDriver(
    url,
    properties,
    schema.synchronous(),
    migrateEmptySchema,
    *callbacks
)
