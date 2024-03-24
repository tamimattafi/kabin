package com.attafitamim.kabin.core.converters.adapters.real

import app.cash.sqldelight.ColumnAdapter

object FloatDoubleAdapter : ColumnAdapter<Float, Double> {
    override fun decode(databaseValue: Double): Float =
        databaseValue.toFloat()

    override fun encode(value: Float): Double =
        value.toDouble()
}
