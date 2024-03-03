package com.attafitamim.kabin.compiler.sql.syntax

import com.attafitamim.kabin.specs.dao.DaoParameterSpec

data class SQLDaoQuery(
    val value: String,
    val parameters: List<DaoParameterSpec>
)
