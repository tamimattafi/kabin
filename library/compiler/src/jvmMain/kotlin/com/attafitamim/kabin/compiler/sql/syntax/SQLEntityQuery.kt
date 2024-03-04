package com.attafitamim.kabin.compiler.sql.syntax

import com.attafitamim.kabin.specs.column.ColumnSpec

data class SQLEntityQuery(
    val value: String,
    val columns: Collection<ColumnSpec>,
    val parametersSize: Int
)
