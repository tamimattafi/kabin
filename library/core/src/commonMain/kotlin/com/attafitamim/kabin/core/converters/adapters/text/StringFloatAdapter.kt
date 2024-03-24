package com.attafitamim.kabin.core.converters.adapters.text

import app.cash.sqldelight.ColumnAdapter

object StringFloatAdapter : ColumnAdapter<String, Float> {
    override fun decode(databaseValue: Float): String =
        databaseValue.toString()

    override fun encode(value: String): Float =
        value.toFloat()
}
