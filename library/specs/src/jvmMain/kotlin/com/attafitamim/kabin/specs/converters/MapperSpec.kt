package com.attafitamim.kabin.specs.converters

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

data class MapperSpec(
    val declaration: KSClassDeclaration,
    val returnType: KSType
)
