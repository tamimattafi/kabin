package com.attafitamim.kabin.core.database.configuration

import app.cash.sqldelight.db.AfterVersion
import java.util.Properties

actual class KabinDatabaseConfiguration(
    val url: String,
    val properties: Properties = Properties(),
    val migrateEmptySchema: Boolean = false,
    val callbacks: Array<out AfterVersion> = emptyArray(),
    actual val extendedConfig: KabinExtendedConfig = KabinExtendedConfig()
)