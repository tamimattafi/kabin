package com.attafitamim.kabin.core.converters.mappers

import app.cash.sqldelight.db.SqlCursor
import com.attafitamim.kabin.core.table.KabinMapper

object StringMapper : KabinMapper<String> {
    override fun map(cursor: SqlCursor): String =
        cursor.getString(0)!!
}
