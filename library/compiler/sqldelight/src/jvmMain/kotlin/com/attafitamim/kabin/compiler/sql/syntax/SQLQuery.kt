package com.attafitamim.kabin.compiler.sql.syntax

data class SQLQuery(
    val value: String,
    val parameters: Collection<String>
)