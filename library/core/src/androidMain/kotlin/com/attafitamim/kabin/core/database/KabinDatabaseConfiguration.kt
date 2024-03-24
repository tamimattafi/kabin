package com.attafitamim.kabin.core.database

import android.content.Context

private const val DEFAULT_CACHE_SIZE = 20

actual class KabinDatabaseConfiguration(
    val context: Context,
    val name: String? = null,
    val cacheSize: Int = DEFAULT_CACHE_SIZE,
    val useNoBackupDirectory: Boolean = false,
    val windowSizeBytes: Long? = null,
)
