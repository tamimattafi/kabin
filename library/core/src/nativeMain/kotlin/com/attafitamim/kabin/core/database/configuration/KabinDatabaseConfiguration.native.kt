package com.attafitamim.kabin.core.database.configuration

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import co.touchlab.sqliter.DatabaseConfiguration
import co.touchlab.sqliter.JournalMode

typealias OnConfiguration = (DatabaseConfiguration) -> DatabaseConfiguration

actual class KabinDatabaseConfiguration(
    val name: String,
    val maxReaderConnections: Int = 1,
    val callbacks: Array<out AfterVersion> = emptyArray(),
    actual val extendedConfig: KabinExtendedConfig = KabinExtendedConfig(),
    val loggingConfig: DatabaseConfiguration.Logging = DatabaseConfiguration.Logging(),
    val lifecycleConfig: DatabaseConfiguration.Lifecycle = DatabaseConfiguration.Lifecycle(),
    val encryptionConfig: DatabaseConfiguration.Encryption = DatabaseConfiguration.Encryption(),
    val inMemory: Boolean = false,
    val journalMode: JournalMode = JournalMode.WAL,
    val onConfiguration: OnConfiguration = createConfiguration(
        extendedConfig,
        loggingConfig,
        lifecycleConfig,
        encryptionConfig,
        inMemory,
        journalMode
    )
) {

    actual fun createSqlDriver(
        schema: SqlSchema<QueryResult.AsyncValue<Unit>>
    ): SqlDriver = NativeSqliteDriver(
        schema.synchronous(),
        name,
        maxReaderConnections,
        onConfiguration,
        *callbacks
    ).configure(this)
}

private fun createConfiguration(
    extendedConfig: KabinExtendedConfig,
    loggingConfig: DatabaseConfiguration.Logging,
    lifecycleConfig: DatabaseConfiguration.Lifecycle,
    encryptionConfig: DatabaseConfiguration.Encryption,
    inMemory: Boolean,
    journalMode: JournalMode,
): OnConfiguration = { config ->
    val newExtendedConfig = config.extendedConfig.copy(
        foreignKeyConstraints = false,
        busyTimeout = extendedConfig.busyTimeout,
        pageSize = extendedConfig.pageSize,
        basePath = extendedConfig.basePath,
        synchronousFlag = extendedConfig.synchronousFlag,
        recursiveTriggers = extendedConfig.recursiveTriggers,
        lookasideSlotSize = extendedConfig.lookasideSlotSize,
        lookasideSlotCount = extendedConfig.lookasideSlotCount,
    )

    config.copy(
        extendedConfig = newExtendedConfig,
        loggingConfig = loggingConfig,
        lifecycleConfig = lifecycleConfig,
        encryptionConfig = encryptionConfig,
        inMemory = inMemory,
        journalMode = journalMode
    )
}