package com.attafitamim.kabin.specs.dao

import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference

data class DaoReturnTypeSpec(
    val reference: KSTypeReference,
    val declaration: KSClassDeclaration,
    val isNullable: Boolean,
    val dataType: DataType
) {

    sealed interface DataType {

        sealed interface Data : DataType

        sealed interface Wrapper : DataType {
            val wrappedDeclaration: DaoReturnTypeSpec
        }

        data object Class : Data

        data class Entity(
            val spec: EntitySpec
        ) : Data

        data class Collection(
            override val wrappedDeclaration: DaoReturnTypeSpec
        ) : Wrapper

        data class Stream(
            override val wrappedDeclaration: DaoReturnTypeSpec
        ) : Wrapper
    }
}
