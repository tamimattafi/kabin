package com.attafitamim.kabin.local.converter

import app.cash.sqldelight.db.SqlCursor
import com.attafitamim.kabin.core.table.KabinMapper
import com.attafitamim.kabin.local.entities.MarriedCount

object MarriedCountMapper : KabinMapper<MarriedCount> {

    override fun map(cursor: SqlCursor): MarriedCount =
        MarriedCount(cursor.getLong(0)!!.toInt())
}
