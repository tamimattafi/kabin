package com.attafitamim.kabin.compiler.sql.syntax

import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.dao.DaoParameterSpec

sealed interface SQLQuery {
    val value: String
    val parametersSize: Int
    val queriedKeys: Set<String>

    data class Parameters(
        override val value: String,
        override val parametersSize: Int,
        val parameters: Collection<DaoParameterSpec>,
        val mutatedKeys: Set<String>,
        override val queriedKeys: Set<String>
    ): SQLQuery

    data class Columns(
        override val value: String,
        override val parametersSize: Int,
        val columns: Collection<ColumnSpec>,
        val mutatedKeys: Set<String>,
        override val queriedKeys: Set<String>
    ): SQLQuery

    data class Raw(
        val rawQueryParameter: DaoParameterSpec,
        override val queriedKeys: Set<String>
    ): SQLQuery {
        override val value: String = rawQueryParameter.name
        override val parametersSize: Int = 0
    }
}
