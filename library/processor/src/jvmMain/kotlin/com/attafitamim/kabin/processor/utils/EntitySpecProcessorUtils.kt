package com.attafitamim.kabin.processor.utils

import com.attafitamim.kabin.processor.spec.EntitySpecProcessor
import com.attafitamim.kabin.specs.core.TypeDeclaration
import com.google.devtools.ksp.symbol.KSClassDeclaration

fun EntitySpecProcessor.getTypeSpec(classDeclaration: KSClassDeclaration): TypeDeclaration {
    if (!hasEntityAnnotation(classDeclaration)) {
        return TypeDeclaration.Class(classDeclaration)
    }

    val entitySpec = getEntitySpec(classDeclaration)
    return TypeDeclaration.Entity(entitySpec)
}
