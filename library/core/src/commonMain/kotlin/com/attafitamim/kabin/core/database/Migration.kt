package com.attafitamim.kabin.core.database

import app.cash.sqldelight.db.SqlDriver

abstract class Migration(
    val startVersion: Int,
    val endVersion: Int
) {

    abstract suspend fun migrate(sqlDriver: SqlDriver)
}
