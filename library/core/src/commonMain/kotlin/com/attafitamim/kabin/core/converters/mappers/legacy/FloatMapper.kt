package com.attafitamim.kabin.core.converters.mappers.legacy

import app.cash.sqldelight.db.SqlCursor
import com.attafitamim.kabin.core.table.KabinMapper

object FloatMapper : KabinMapper<Float> {
    override fun map(cursor: SqlCursor): Float =
        cursor.getDouble(0)?.toFloat() ?: 0f
}
