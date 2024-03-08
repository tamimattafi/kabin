package com.attafitamim.kabin.compiler.sql.utils.sql.dao

import com.attafitamim.kabin.compiler.sql.generator.references.ParameterReference
import com.attafitamim.kabin.compiler.sql.syntax.SQLQuery
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.toParameterReferences
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.toReference
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.toReferences

fun SQLQuery.getParameterReferences(): List<ParameterReference> = when (this) {
    is SQLQuery.Columns -> columns.toParameterReferences()
    is SQLQuery.Parameters -> parameters.toReferences()
    is SQLQuery.Raw -> listOf(rawQueryParameter.toReference())
}
