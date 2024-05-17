package com.attafitamim.kabin.core.database.configuration

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties

actual class KabinDatabaseConfiguration(
    val url: String,
    val properties: Properties = Properties(),
    val migrateEmptySchema: Boolean = false,
    val callbacks: Array<out AfterVersion> = emptyArray(),
    actual val extendedConfig: KabinExtendedConfig = KabinExtendedConfig()
) {

    actual fun createSqlDriver(
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>
    ): SqlDriver = JdbcSqliteDriver(
        url,
        properties,
        schema.synchronous(),
        migrateEmptySchema,
        *callbacks
    ).configure(this)
}