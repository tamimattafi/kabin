package com.attafitamim.kabin.core.converters.adapters.text

import app.cash.sqldelight.ColumnAdapter

object LongStringAdapter : ColumnAdapter<Long, String> {
    override fun decode(databaseValue: String): Long =
        databaseValue.toLong()

    override fun encode(value: Long): String =
        value.toString()
}
