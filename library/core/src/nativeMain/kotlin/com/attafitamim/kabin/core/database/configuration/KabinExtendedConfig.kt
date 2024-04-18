package com.attafitamim.kabin.core.database.configuration

import co.touchlab.sqliter.SynchronousFlag

actual class KabinExtendedConfig(
    actual val foreignKeyConstraintsEnabled: Boolean = true,
    actual val deferForeignKeysInsideTransaction: Boolean = true,
    val busyTimeout: Int = 5000,
    val pageSize: Int? = null,
    val basePath: String? = null,
    val synchronousFlag: SynchronousFlag? = null,
    val recursiveTriggers: Boolean = false,
    val lookasideSlotSize: Int = -1,
    val lookasideSlotCount: Int = -1,
)