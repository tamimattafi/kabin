package com.attafitamim.kabin.core.database.configuration

import app.cash.sqldelight.db.AfterVersion
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
)

private fun createConfiguration(
    extendedConfig: KabinExtendedConfig,
    loggingConfig: DatabaseConfiguration.Logging,
    lifecycleConfig: DatabaseConfiguration.Lifecycle,
    encryptionConfig: DatabaseConfiguration.Encryption,
    inMemory: Boolean,
    journalMode: JournalMode,
): OnConfiguration = { config ->
    val newExtendedConfig = config.extendedConfig.copy(
        foreignKeyConstraints = extendedConfig.foreignKeyConstraintsEnabled,
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