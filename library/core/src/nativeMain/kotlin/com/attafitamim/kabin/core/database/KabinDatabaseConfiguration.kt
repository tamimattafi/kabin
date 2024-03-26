package com.attafitamim.kabin.core.database

import app.cash.sqldelight.db.AfterVersion
import co.touchlab.sqliter.DatabaseConfiguration

typealias OnConfiguration = (DatabaseConfiguration) -> DatabaseConfiguration

private fun createConfiguration(
    foreignKeyConstraintsEnabled: Boolean
): OnConfiguration = { config ->
    val extendedConfig = DatabaseConfiguration.Extended(
        foreignKeyConstraints = foreignKeyConstraintsEnabled
    )

    config.copy(extendedConfig = extendedConfig)
}

actual class KabinDatabaseConfiguration(
    val name: String,
    val maxReaderConnections: Int = 1,
    val callbacks: Array<out AfterVersion> = emptyArray(),
    val foreignKeyConstraintsEnabled: Boolean = true,
    val onConfiguration: OnConfiguration = createConfiguration(foreignKeyConstraintsEnabled)
)