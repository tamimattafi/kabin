package com.attafitamim.kabin.core.converters

import app.cash.sqldelight.ColumnAdapter

object FloatDoubleConverter : ColumnAdapter<Float, Double> {
    override fun decode(databaseValue: Double): Float =
        databaseValue.toFloat()

    override fun encode(value: Float): Double =
        value.toDouble()
}
