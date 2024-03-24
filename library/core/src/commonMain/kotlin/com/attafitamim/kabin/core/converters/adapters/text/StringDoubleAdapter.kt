package com.attafitamim.kabin.core.converters.adapters.text

import app.cash.sqldelight.ColumnAdapter

object StringDoubleAdapter : ColumnAdapter<String, Double> {
    override fun decode(databaseValue: Double): String =
        databaseValue.toString()

    override fun encode(value: String): Double =
        value.toDouble()
}
