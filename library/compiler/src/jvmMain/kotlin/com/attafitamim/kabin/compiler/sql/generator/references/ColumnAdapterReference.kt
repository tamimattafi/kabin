package com.attafitamim.kabin.compiler.sql.generator.references

import com.google.devtools.ksp.symbol.ClassKind
import com.squareup.kotlinpoet.TypeName

data class ColumnAdapterReference(
    val affinityType: TypeName,
    val kotlinType: TypeName,
    val kotlinTypeKind: ClassKind
)
