package com.attafitamim.kabin.core.table

import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver

interface KabinTable {
    suspend fun create(sqlDriver: SqlDriver)
    suspend fun drop(sqlDriver: SqlDriver)
    suspend fun clear(sqlDriver: SqlDriver)

    fun interface EntityMapper<T> {
        fun map(cursor: SqlCursor): T
    }
}
