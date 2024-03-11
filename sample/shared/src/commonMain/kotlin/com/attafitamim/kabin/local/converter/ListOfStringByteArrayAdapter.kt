package com.attafitamim.kabin.local.converter

import app.cash.sqldelight.ColumnAdapter

object ListOfStringByteArrayAdapter : ColumnAdapter<List<String>, String> {

    private const val ELEMENT_SEPARATOR = "<||>"
    override fun decode(databaseValue: String): List<String> =
        databaseValue.split(ELEMENT_SEPARATOR)

    override fun encode(value: List<String>): String =
        value.joinToString(ELEMENT_SEPARATOR)
}
