package com.attafitamim.kabin.core.converters

import app.cash.sqldelight.ColumnAdapter

object IntLongConverter : ColumnAdapter<Int, Long> {
    override fun decode(databaseValue: Long): Int =
        databaseValue.toInt()

    override fun encode(value: Int): Long =
        value.toLong()
}
