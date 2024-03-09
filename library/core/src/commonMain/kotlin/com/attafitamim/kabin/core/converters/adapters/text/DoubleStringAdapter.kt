package com.attafitamim.kabin.core.converters.adapters.text

import app.cash.sqldelight.ColumnAdapter

object DoubleStringAdapter : ColumnAdapter<Double, String> {
    override fun decode(databaseValue: String): Double =
        databaseValue.toDouble()

    override fun encode(value: Double): String =
        value.toString()
}
