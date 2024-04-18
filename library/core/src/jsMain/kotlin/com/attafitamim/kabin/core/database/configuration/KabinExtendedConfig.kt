package com.attafitamim.kabin.core.database.configuration

actual class KabinExtendedConfig(
    actual val foreignKeyConstraintsEnabled: Boolean = true,
    actual val deferForeignKeysInsideTransaction: Boolean = true
)
