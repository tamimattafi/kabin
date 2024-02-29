package com.attafitamim.kabin.core.table

import app.cash.sqldelight.db.SqlCursor

fun interface KabinMapper<T> {
    fun map(cursor: SqlCursor): T
}
