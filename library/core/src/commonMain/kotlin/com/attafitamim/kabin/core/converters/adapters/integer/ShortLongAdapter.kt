package com.attafitamim.kabin.core.converters.adapters.integer

import app.cash.sqldelight.ColumnAdapter

object ShortLongAdapter : ColumnAdapter<Short, Long> {
    override fun decode(databaseValue: Long): Short =
        databaseValue.toShort()

    override fun encode(value: Short): Long =
        value.toLong()
}
