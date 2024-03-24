package com.attafitamim.kabin.core.converters.adapters.text

import app.cash.sqldelight.ColumnAdapter

object StringLongAdapter : ColumnAdapter<String, Long> {
    override fun decode(databaseValue: Long): String =
        databaseValue.toString()

    override fun encode(value: String): Long =
        value.toLong()
}
