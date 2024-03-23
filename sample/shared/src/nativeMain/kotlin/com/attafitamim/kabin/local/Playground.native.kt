package com.attafitamim.kabin.local

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual fun createDriver(
    configuration: PlaygroundConfiguration,
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
    name: String
): SqlDriver = NativeSqliteDriver(schema.synchronous(), name)
