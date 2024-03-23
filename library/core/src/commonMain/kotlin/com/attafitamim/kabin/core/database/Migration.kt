package com.attafitamim.kabin.core.database

import app.cash.sqldelight.db.SqlDriver

abstract class Migration(
    val startVersion: Long,
    val endVersion: Long
) {

    abstract suspend fun migrate(sqlDriver: SqlDriver)
}
