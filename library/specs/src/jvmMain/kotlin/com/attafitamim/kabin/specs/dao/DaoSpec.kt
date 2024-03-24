package com.attafitamim.kabin.specs.dao

import com.google.devtools.ksp.symbol.KSClassDeclaration

data class DaoSpec(
    val declaration: KSClassDeclaration,
    val functionSpecs: List<DaoFunctionSpec>
)
