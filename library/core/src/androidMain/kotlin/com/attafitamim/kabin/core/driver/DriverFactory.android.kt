package com.attafitamim.kabin.core.driver

import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.attafitamim.kabin.core.database.configuration.KabinDatabaseConfiguration
import com.attafitamim.kabin.core.database.configuration.OpenCallback

private fun createCallback(
    schema: SqlSchema<QueryResult.Value<Unit>>,
    onOpen: OpenCallback?,
) = object : AndroidSqliteDriver.Callback(schema) {
    override fun onOpen(db: SupportSQLiteDatabase) {
        onOpen?.invoke(db)
    }
}

actual fun KabinDatabaseConfiguration.createDriver(
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
): SqlDriver {
    val synchronousSchema = schema.synchronous()
    return AndroidSqliteDriver(
        synchronousSchema,
        context,
        name,
        cacheSize = cacheSize,
        useNoBackupDirectory = useNoBackupDirectory,
        windowSizeBytes = windowSizeBytes,
        callback = createCallback(synchronousSchema, onOpen)
    )
}
