package com.attafitamim.kabin.compiler.sql.syntax

data class SQLSimpleQuery(
    val value: String,
    val parameters: Collection<String>
)
