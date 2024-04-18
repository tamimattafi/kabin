package com.attafitamim.kabin.core.driver

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.attafitamim.kabin.core.database.configuration.KabinDatabaseConfiguration

actual fun KabinDatabaseConfiguration.createDriver(
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
): SqlDriver = NativeSqliteDriver(
    schema.synchronous(),
    name,
    maxReaderConnections,
    onConfiguration,
    *callbacks
)