package com.attafitamim.kabin.compiler.sql.syntax

import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.dao.DaoParameterSpec

sealed interface SQLQuery {
    val value: String
    val parametersSize: Int

    data class Parameters(
        override val value: String,
        override val parametersSize: Int,
        val parameters: Collection<DaoParameterSpec>,
    ): SQLQuery

    data class Columns(
        override val value: String,
        override val parametersSize: Int,
        val columns: Collection<ColumnSpec>,
    ): SQLQuery

    data class Raw(
        val rawQueryParameter: DaoParameterSpec
    ): SQLQuery {
        override val value: String = rawQueryParameter.name
        override val parametersSize: Int = 0
    }
}
