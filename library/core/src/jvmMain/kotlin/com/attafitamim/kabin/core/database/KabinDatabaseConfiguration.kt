package com.attafitamim.kabin.core.database

import app.cash.sqldelight.db.AfterVersion
import java.util.Properties

actual class KabinDatabaseConfiguration(
    val url: String,
    val properties: Properties = Properties(),
    val migrateEmptySchema: Boolean = false,
    val callbacks: Array<out AfterVersion> = emptyArray(),
    val foreignKeyConstraintsEnabled: Boolean = true
)
