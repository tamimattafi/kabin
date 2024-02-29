package com.attafitamim.kabin.core.table

import app.cash.sqldelight.db.SqlCursor

fun interface KabinEntityMapper<T> {
    fun map(cursor: SqlCursor): T
}
