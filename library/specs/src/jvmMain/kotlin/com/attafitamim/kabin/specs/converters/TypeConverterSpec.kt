package com.attafitamim.kabin.specs.converters

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

data class TypeConverterSpec(
    val declaration: KSClassDeclaration,
    val affinityType: KSType,
    val kotlinType: KSType
)
