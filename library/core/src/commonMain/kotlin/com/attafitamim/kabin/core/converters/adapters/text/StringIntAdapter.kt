package com.attafitamim.kabin.core.converters.adapters.text

import app.cash.sqldelight.ColumnAdapter

object StringIntAdapter : ColumnAdapter<String, Int> {
    override fun decode(databaseValue: Int): String =
        databaseValue.toString()

    override fun encode(value: String): Int =
        value.toInt()
}
