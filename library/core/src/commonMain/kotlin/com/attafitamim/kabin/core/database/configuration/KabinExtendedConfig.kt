package com.attafitamim.kabin.core.database.configuration

expect class KabinExtendedConfig {
    val foreignKeyConstraintsEnabled: Boolean
    val deferForeignKeysInsideTransaction: Boolean
}