package com.attafitamim.kabin.specs.core

import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.symbol.KSClassDeclaration

sealed interface TypeDeclaration {
    val declaration: KSClassDeclaration

    data class Entity(
        val spec: EntitySpec
    ) : TypeDeclaration {
        override val declaration: KSClassDeclaration get() = spec.declaration
    }

    data class EntityList(
        val spec: EntitySpec
    ) : TypeDeclaration {
        override val declaration: KSClassDeclaration get() = spec.declaration
    }

    data class Class(
        override val declaration: KSClassDeclaration
    ) : TypeDeclaration
}