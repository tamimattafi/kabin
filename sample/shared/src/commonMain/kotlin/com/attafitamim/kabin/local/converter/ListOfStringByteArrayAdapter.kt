package com.attafitamim.kabin.local.converter

import app.cash.sqldelight.ColumnAdapter

object ListOfStringByteArrayAdapter : ColumnAdapter<List<String>, ByteArray> {

    private const val ELEMENT_SEPARATOR = "<||>"
    override fun decode(databaseValue: ByteArray): List<String> =
        databaseValue.decodeToString().split(ELEMENT_SEPARATOR)

    override fun encode(value: List<String>): ByteArray =
        value.joinToString(ELEMENT_SEPARATOR).encodeToByteArray()
}
