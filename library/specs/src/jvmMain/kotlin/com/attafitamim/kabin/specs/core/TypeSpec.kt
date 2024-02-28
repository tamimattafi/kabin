package com.attafitamim.kabin.specs.core

import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.symbol.KSClassDeclaration

sealed interface TypeSpec {
    val declaration: KSClassDeclaration

    data class Entity(
        val spec: EntitySpec
    ) : TypeSpec {
        override val declaration: KSClassDeclaration get() = spec.declaration
    }

    data class Class(
        override val declaration: KSClassDeclaration
    ) : TypeSpec
}