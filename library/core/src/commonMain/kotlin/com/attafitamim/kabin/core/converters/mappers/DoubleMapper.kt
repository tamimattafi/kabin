package com.attafitamim.kabin.core.converters.mappers

import app.cash.sqldelight.db.SqlCursor
import com.attafitamim.kabin.core.table.KabinMapper

object DoubleMapper : KabinMapper<Double> {
    override fun map(cursor: SqlCursor): Double =
        cursor.getDouble(0)!!
}
