package com.attafitamim.kabin.compiler.sql.generator.references

import com.squareup.kotlinpoet.TypeName

data class ParameterReference(
    val name: String,
    val type: TypeName
)

