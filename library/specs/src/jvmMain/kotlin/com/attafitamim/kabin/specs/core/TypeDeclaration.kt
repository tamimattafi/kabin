package com.attafitamim.kabin.specs.core

import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.symbol.KSClassDeclaration

sealed interface TypeDeclaration {
    val declaration: KSClassDeclaration
    val isNullable: Boolean

    data class Entity(
        val spec: EntitySpec,
        override val isNullable: Boolean
    ) : TypeDeclaration {
        override val declaration: KSClassDeclaration get() = spec.declaration
    }

    data class Class(
        override val declaration: KSClassDeclaration,
        override val isNullable: Boolean
    ) : TypeDeclaration

    data class List(
        val elementDeclaration: TypeDeclaration,
        override val isNullable: Boolean
    ) : TypeDeclaration {
        override val declaration: KSClassDeclaration get() = elementDeclaration.declaration
    }

    data class Flow(
        val elementDeclaration: TypeDeclaration,
        override val isNullable: Boolean
    ) : TypeDeclaration {
        override val declaration: KSClassDeclaration get() = elementDeclaration.declaration
    }
}