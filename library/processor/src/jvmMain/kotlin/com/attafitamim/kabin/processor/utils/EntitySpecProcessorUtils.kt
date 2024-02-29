package com.attafitamim.kabin.processor.utils

import com.attafitamim.kabin.processor.spec.EntitySpecProcessor
import com.attafitamim.kabin.specs.core.TypeDeclaration
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeArgument
import kotlinx.coroutines.flow.Flow

fun EntitySpecProcessor.getTypeSpec(type: KSType): TypeDeclaration? {
    val classDeclaration = type.declaration as KSClassDeclaration
    when (classDeclaration.qualifiedName?.asString()) {
        Flow::class.qualifiedName -> {
            val typeDeclaration = getTypeSpec(type.arguments.first())
            return TypeDeclaration.Flow(typeDeclaration)
        }

        List::class.qualifiedName -> {
            val typeDeclaration = getTypeSpec(type.arguments.first())
            return TypeDeclaration.List(typeDeclaration)
        }

        Unit::class.qualifiedName,
        null -> return null
    }

    return getTypeSpec(classDeclaration)
}

fun EntitySpecProcessor.getTypeSpec(typeArgument: KSTypeArgument): TypeDeclaration {
    val type = requireNotNull(typeArgument.type).resolve()
    val typeDeclaration = getTypeSpec(type)
    return requireNotNull(typeDeclaration)
}

fun EntitySpecProcessor.getTypeSpec(classDeclaration: KSClassDeclaration): TypeDeclaration {
    if (!hasEntityAnnotation(classDeclaration)) {
        return TypeDeclaration.Class(classDeclaration)
    }

    val entitySpec = getEntitySpec(classDeclaration)
    return TypeDeclaration.Entity(entitySpec)
}
