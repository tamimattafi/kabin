package com.attafitamim.kabin.core.converters.mappers.legacy

import app.cash.sqldelight.db.SqlCursor
import com.attafitamim.kabin.core.table.KabinMapper

object LongMapper : KabinMapper<Long> {
    override fun map(cursor: SqlCursor): Long =
        cursor.getLong(0) ?: 0L
}
