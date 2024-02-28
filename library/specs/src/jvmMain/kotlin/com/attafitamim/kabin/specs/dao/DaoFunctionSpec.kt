package com.attafitamim.kabin.specs.dao

import com.attafitamim.kabin.specs.core.TypeDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

data class DaoFunctionSpec(
    val declaration: KSFunctionDeclaration,
    val parameters: List<DaoFunctionParameterSpec>,
    val transactionSpec: TransactionSpec?,
    val actionSpec: DaoActionSpec?,
    val returnType: TypeDeclaration?
)
