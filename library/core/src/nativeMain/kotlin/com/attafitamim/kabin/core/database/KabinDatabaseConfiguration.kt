package com.attafitamim.kabin.core.database

import app.cash.sqldelight.db.AfterVersion
import co.touchlab.sqliter.DatabaseConfiguration

actual class KabinDatabaseConfiguration(
    val name: String,
    val maxReaderConnections: Int = 1,
    val onConfiguration: (DatabaseConfiguration) -> DatabaseConfiguration = { it },
    val callbacks: Array<out AfterVersion> = emptyArray()
)