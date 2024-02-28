package com.attafitamim.kabin.processor.utils

import com.attafitamim.kabin.processor.spec.EntitySpecProcessor
import com.attafitamim.kabin.specs.core.TypeSpec
import com.google.devtools.ksp.symbol.KSClassDeclaration

fun EntitySpecProcessor.getTypeSpec(classDeclaration: KSClassDeclaration): TypeSpec {
    if (!hasEntityAnnotation(classDeclaration)) {
        return TypeSpec.Class(classDeclaration)
    }

    val entitySpec = getEntitySpec(classDeclaration)
    return TypeSpec.Entity(entitySpec)
}
