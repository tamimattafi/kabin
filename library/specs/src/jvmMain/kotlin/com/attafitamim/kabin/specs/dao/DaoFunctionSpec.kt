package com.attafitamim.kabin.specs.dao

import com.google.devtools.ksp.symbol.KSFunctionDeclaration

data class DaoFunctionSpec(
    val declaration: KSFunctionDeclaration,
    val transactionSpec: TransactionSpec?,
    val actionSpec: DaoActionSpec?
)
