package com.attafitamim.kabin.local

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

actual fun createDriver(
    configuration: PlaygroundConfiguration,
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
    name: String
): SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, schema = schema.synchronous())
