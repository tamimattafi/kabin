package com.attafitamim.kabin.core.converters.adapters

import app.cash.sqldelight.ColumnAdapter

object FloatDoubleAdapter : ColumnAdapter<Float, Double> {
    override fun decode(databaseValue: Double): Float =
        databaseValue.toFloat()

    override fun encode(value: Float): Double =
        value.toDouble()
}
