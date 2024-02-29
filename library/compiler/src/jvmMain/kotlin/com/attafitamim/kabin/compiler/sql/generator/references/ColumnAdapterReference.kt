package com.attafitamim.kabin.compiler.sql.generator.references

import com.squareup.kotlinpoet.ClassName

data class ColumnAdapterReference(
    val affinityType: ClassName,
    val kotlinType: ClassName
)
