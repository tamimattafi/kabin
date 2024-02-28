package com.attafitamim.kabin.specs.dao

import com.attafitamim.kabin.specs.core.TypeDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter

class DaoFunctionParameterSpec(
    val declaration: KSValueParameter,
    val name: String,
    val isNullable: Boolean,
    val typeDeclaration: TypeDeclaration
)
