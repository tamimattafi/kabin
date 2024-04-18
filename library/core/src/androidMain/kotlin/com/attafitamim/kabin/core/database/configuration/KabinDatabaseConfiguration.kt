package com.attafitamim.kabin.core.database.configuration

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase

private const val DEFAULT_CACHE_SIZE = 20

typealias OpenCallback = (db: SupportSQLiteDatabase) -> Unit

actual class KabinDatabaseConfiguration(
    val context: Context,
    val name: String? = null,
    val cacheSize: Int = DEFAULT_CACHE_SIZE,
    val useNoBackupDirectory: Boolean = false,
    val windowSizeBytes: Long? = null,
    actual val extendedConfig: KabinExtendedConfig = KabinExtendedConfig(),
    val onOpen: OpenCallback? = createDefaultCallback(extendedConfig)
)

private fun createDefaultCallback(
    constraintsConfiguration: KabinExtendedConfig
): OpenCallback = { db ->
    db.setForeignKeyConstraintsEnabled(constraintsConfiguration.foreignKeyConstraintsEnabled)
}
