package com.attafitamim.kabin.specs.column

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference

data class ColumnTypeSpec(
    val reference: KSTypeReference,
    val declaration: KSClassDeclaration,
    val isNullable: Boolean,
    val dataType: DataType
) {

    sealed interface DataType {

        data class Embedded(
            val prefix: String?,
            val columns: List<ColumnSpec>,
        ) : DataType

        data object Class : DataType
    }
}