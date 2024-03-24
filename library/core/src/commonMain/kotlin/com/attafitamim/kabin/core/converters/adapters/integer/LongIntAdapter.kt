package com.attafitamim.kabin.core.converters.adapters.integer

import app.cash.sqldelight.ColumnAdapter

object LongIntAdapter : ColumnAdapter<Long, Int> {
    override fun decode(databaseValue: Int): Long =
        databaseValue.toLong()

    override fun encode(value: Long): Int =
        value.toInt()
}
