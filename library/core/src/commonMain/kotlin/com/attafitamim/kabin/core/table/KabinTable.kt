package com.attafitamim.kabin.core.table

import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver

interface KabinTable {
    suspend fun create(driver: SqlDriver)
    suspend fun drop(driver: SqlDriver)
    suspend fun clear(driver: SqlDriver)

    fun interface Mapper<T> {
        fun map(cursor: SqlCursor): T
    }
}
