package com.attafitamim.kabin.core.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase

private const val DEFAULT_CACHE_SIZE = 20

typealias OpenCallback = (db: SupportSQLiteDatabase) -> Unit

private fun createDefaultCallback(
    foreignKeyConstraintsEnabled: Boolean
): OpenCallback = { db ->
    db.setForeignKeyConstraintsEnabled(foreignKeyConstraintsEnabled)
}

actual class KabinDatabaseConfiguration(
    val context: Context,
    val name: String? = null,
    val cacheSize: Int = DEFAULT_CACHE_SIZE,
    val useNoBackupDirectory: Boolean = false,
    val windowSizeBytes: Long? = null,
    val foreignKeyConstraintsEnabled: Boolean = true,
    val onOpen: OpenCallback? = createDefaultCallback(foreignKeyConstraintsEnabled)
)
