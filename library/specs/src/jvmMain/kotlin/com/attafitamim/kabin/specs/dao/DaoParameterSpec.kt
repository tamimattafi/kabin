package com.attafitamim.kabin.specs.dao

import com.google.devtools.ksp.symbol.KSValueParameter

data class DaoParameterSpec(
    val declaration: KSValueParameter,
    val name: String,
    val typeSpec: DataTypeSpec
)
