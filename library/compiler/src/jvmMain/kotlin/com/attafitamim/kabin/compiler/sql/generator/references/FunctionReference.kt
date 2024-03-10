package com.attafitamim.kabin.compiler.sql.generator.references

data class FunctionReference(
    val name: String,
    val parameters: List<ParameterReference>
)
