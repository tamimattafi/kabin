package com.attafitamim.kabin.core.converters.mappers

import app.cash.sqldelight.db.SqlCursor
import com.attafitamim.kabin.core.table.KabinMapper

object BytesMapper : KabinMapper<ByteArray> {
    override fun map(cursor: SqlCursor): ByteArray =
        cursor.getBytes(0)!!
}
