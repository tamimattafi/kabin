package com.attafitamim.kabin.specs.dao

import com.google.devtools.ksp.symbol.KSFunctionDeclaration

data class DaoFunctionSpec(
    val declaration: KSFunctionDeclaration,
    val parameters: List<DaoParameterSpec>,
    val transactionSpec: TransactionSpec?,
    val actionSpec: DaoActionSpec?,
    val returnTypeSpec: DataTypeSpec?
)
