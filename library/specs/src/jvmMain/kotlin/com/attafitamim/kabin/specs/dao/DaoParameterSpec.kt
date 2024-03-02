package com.attafitamim.kabin.specs.dao

import com.google.devtools.ksp.symbol.KSValueParameter

class DaoParameterSpec(
    val declaration: KSValueParameter,
    val name: String,
    val isNullable: Boolean,
    val daoReturnTypeSpec: DaoReturnTypeSpec
)
