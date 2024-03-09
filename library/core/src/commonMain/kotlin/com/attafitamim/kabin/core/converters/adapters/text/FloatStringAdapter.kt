package com.attafitamim.kabin.core.converters.adapters.text

import app.cash.sqldelight.ColumnAdapter

object FloatStringAdapter : ColumnAdapter<Float, String> {
    override fun decode(databaseValue: String): Float =
        databaseValue.toFloat()

    override fun encode(value: Float): String =
        value.toString()
}
