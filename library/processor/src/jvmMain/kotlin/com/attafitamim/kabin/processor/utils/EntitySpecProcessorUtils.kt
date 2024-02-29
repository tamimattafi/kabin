package com.attafitamim.kabin.processor.utils

import com.attafitamim.kabin.processor.spec.EntitySpecProcessor
import com.attafitamim.kabin.specs.core.TypeDeclaration
import com.google.devtools.ksp.closestClassDeclaration
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference

fun EntitySpecProcessor.getTypeSpec(
    classDeclaration: KSClassDeclaration,
    typeReference: KSType
): TypeDeclaration? {
    if (classDeclaration.qualifiedName?.asString() == Unit::class.qualifiedName) {
        return null
    }

    if (classDeclaration.qualifiedName?.asString() == List::class.qualifiedName) {
        val elementType = typeReference.arguments.first()
        val elementDeclaration = elementType.type?.resolveClassDeclaration()

        if (elementDeclaration != null && hasEntityAnnotation(elementDeclaration)) {
            val entitySpec = getEntitySpec(elementDeclaration)
            return TypeDeclaration.EntityList(entitySpec)
        }
    }

    if (!hasEntityAnnotation(classDeclaration)) {
        return TypeDeclaration.Class(classDeclaration)
    }

    val entitySpec = getEntitySpec(classDeclaration)
    return TypeDeclaration.Entity(entitySpec)
}
