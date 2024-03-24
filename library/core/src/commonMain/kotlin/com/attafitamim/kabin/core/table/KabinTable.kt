package com.attafitamim.kabin.core.table

import app.cash.sqldelight.db.SqlDriver

interface KabinTable {
    suspend fun create(driver: SqlDriver)
    suspend fun drop(driver: SqlDriver)
    suspend fun clear(driver: SqlDriver)
}
