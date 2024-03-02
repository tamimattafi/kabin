package com.attafitamim.kabin.processor.utils

import com.attafitamim.kabin.processor.spec.EntitySpecProcessor
import com.attafitamim.kabin.specs.core.TypeDeclaration
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import kotlinx.coroutines.flow.Flow

fun EntitySpecProcessor.getTypeSpec(type: KSType): TypeDeclaration? {
    val classDeclaration = type.declaration as KSClassDeclaration
    val isNullable = type.isMarkedNullable
    when (classDeclaration.qualifiedName?.asString()) {
        Flow::class.qualifiedName -> {
            val typeDeclaration = getTypeSpec(type.arguments.first())
            return TypeDeclaration.Flow(typeDeclaration, isNullable)
        }

        List::class.qualifiedName -> {
            val typeDeclaration = getTypeSpec(type.arguments.first())
            return TypeDeclaration.List(typeDeclaration, isNullable)
        }

        Unit::class.qualifiedName,
        null -> return null
    }

    return getTypeSpec(classDeclaration, type.isMarkedNullable)
}

fun EntitySpecProcessor.getTypeSpec(typeArgument: KSTypeArgument): TypeDeclaration {
    val type = requireNotNull(typeArgument.type).resolve()
    val typeDeclaration = getTypeSpec(type)
    return requireNotNull(typeDeclaration)
}

fun EntitySpecProcessor.getTypeSpec(
    classDeclaration: KSClassDeclaration,
    isNullable: Boolean
): TypeDeclaration {
    if (!hasEntityAnnotation(classDeclaration)) {
        return TypeDeclaration.Class(classDeclaration, isNullable)
    }

    val entitySpec = getEntitySpec(classDeclaration)
    return TypeDeclaration.Entity(entitySpec, isNullable)
}
