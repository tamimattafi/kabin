package com.attafitamim.kabin.specs.dao

import com.attafitamim.kabin.specs.core.TypeSpec
import com.google.devtools.ksp.symbol.KSValueParameter

class DaoFunctionParameterSpec(
    val declaration: KSValueParameter,
    val name: String,
    val isNullable: Boolean,
    val typeSpec: TypeSpec
)
