package com.attafitamim.kabin.specs.dao

import com.attafitamim.kabin.specs.entity.EntitySpec
import com.attafitamim.kabin.specs.relation.compound.CompoundSpec
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference

data class DataTypeSpec(
    val reference: KSTypeReference,
    val type: KSType,
    val declaration: KSClassDeclaration,
    val isNullable: Boolean,
    val dataType: DataType
) {

    sealed interface DataType {

        sealed interface Data : DataType

        sealed interface Wrapper : DataType {
            val nestedTypeSpec: DataTypeSpec
        }

        data object Class : Data

        data class Entity(
            val entitySpec: EntitySpec
        ) : Data

        data class Compound(
            val compoundSpec: CompoundSpec
        ) : Data

        data class Collection(
            override val nestedTypeSpec: DataTypeSpec
        ) : Wrapper

        data class Stream(
            override val nestedTypeSpec: DataTypeSpec
        ) : Wrapper
    }
}
