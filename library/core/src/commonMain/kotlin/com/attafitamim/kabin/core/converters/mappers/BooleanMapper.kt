package com.attafitamim.kabin.core.converters.mappers

import app.cash.sqldelight.db.SqlCursor
import com.attafitamim.kabin.core.table.KabinMapper

object BooleanMapper : KabinMapper<Boolean> {
    override fun map(cursor: SqlCursor): Boolean =
        cursor.getBoolean(0)!!
}
