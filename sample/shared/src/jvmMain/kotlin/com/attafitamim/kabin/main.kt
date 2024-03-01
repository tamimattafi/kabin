package com.attafitamim.kabin

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.attafitamim.kabin.local.Playground
import com.attafitamim.kabin.local.database.SampleDatabase
import com.attafitamim.kabin.local.database.scheme
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun main() {
    val driver = createDatabaseDriver(SampleDatabase::class.scheme.synchronous())

    Playground.scope.launch {
        Playground.useSampleDatabase(driver)
    }

    while (Playground.scope.isActive) {
        Thread.sleep(10000)
    }
}

private fun createDatabaseDriver(
    schema: SqlSchema<QueryResult.Value<Unit>>,
): SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, schema = schema)