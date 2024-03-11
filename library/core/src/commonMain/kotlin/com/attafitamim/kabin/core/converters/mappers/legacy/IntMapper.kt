package com.attafitamim.kabin.core.converters.mappers.legacy

import app.cash.sqldelight.db.SqlCursor
import com.attafitamim.kabin.core.table.KabinMapper

object IntMapper : KabinMapper<Int> {
    override fun map(cursor: SqlCursor): Int =
        cursor.getLong(0)?.toInt() ?: 0
}
