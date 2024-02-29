package com.attafitamim.kabin.core.converters.adapters

import app.cash.sqldelight.ColumnAdapter

object IntLongAdapter : ColumnAdapter<Int, Long> {
    override fun decode(databaseValue: Long): Int =
        databaseValue.toInt()

    override fun encode(value: Int): Long =
        value.toLong()
}
