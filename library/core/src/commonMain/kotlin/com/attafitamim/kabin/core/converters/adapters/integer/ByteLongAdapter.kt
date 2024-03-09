package com.attafitamim.kabin.core.converters.adapters.integer

import app.cash.sqldelight.ColumnAdapter

object ByteLongAdapter : ColumnAdapter<Byte, Long> {
    override fun decode(databaseValue: Long): Byte =
        databaseValue.toByte()

    override fun encode(value: Byte): Long =
        value.toLong()
}
