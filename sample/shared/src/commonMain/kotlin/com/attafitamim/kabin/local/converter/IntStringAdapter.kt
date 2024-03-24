package com.attafitamim.kabin.local.converter

import app.cash.sqldelight.ColumnAdapter

object IntStringAdapter : ColumnAdapter<Int, String> {
    override fun decode(databaseValue: String): Int =
        databaseValue.toInt()

    override fun encode(value: Int): String =
        value.toString()
}
