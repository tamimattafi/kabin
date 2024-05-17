package com.attafitamim.kabin.core.database.configuration

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

private const val DEFAULT_CACHE_SIZE = 20

actual class KabinDatabaseConfiguration(
    val context: Context,
    val name: String? = null,
    val cacheSize: Int = DEFAULT_CACHE_SIZE,
    val useNoBackupDirectory: Boolean = false,
    val windowSizeBytes: Long? = null,
    actual val extendedConfig: KabinExtendedConfig = KabinExtendedConfig()
) {

    private fun createCallback(
        schema: SqlSchema<QueryResult.Value<Unit>>
    ) = object : AndroidSqliteDriver.Callback(schema) {}

    actual fun createSqlDriver(
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
            callback = createCallback(synchronousSchema)
        ).configure(this)
    }
}
