package com.attafitamim.kabin.local.database.migration

import app.cash.sqldelight.db.SqlDriver
import com.attafitamim.kabin.core.database.Migration

class LogMigration(
    startVersion: Long,
    endVersion: Long
) : Migration(startVersion, endVersion) {

    override suspend fun migrate(sqlDriver: SqlDriver) {
        println("MIGRATION: called migrate from $startVersion to $endVersion")
    }
}
