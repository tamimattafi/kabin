package com.attafitamim.kabin.core.converters.adapters.real

import app.cash.sqldelight.ColumnAdapter

object DoubleFloatAdapter : ColumnAdapter<Double, Float> {
    override fun decode(databaseValue: Float): Double =
        databaseValue.toDouble()

    override fun encode(value: Double): Float =
        value.toFloat()
}
