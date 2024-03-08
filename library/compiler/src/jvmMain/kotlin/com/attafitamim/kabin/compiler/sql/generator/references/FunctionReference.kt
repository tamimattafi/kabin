package com.attafitamim.kabin.compiler.sql.generator.references

import com.squareup.kotlinpoet.TypeName

data class FunctionReference(
    val name: String,
    val parameters: List<ParameterReference>,
    val returnType: TypeName?
)
