package com.attafitamim.kabin.processor.utils

import com.attafitamim.kabin.processor.spec.EntitySpecProcessor
import com.attafitamim.kabin.specs.dao.DaoReturnTypeSpec
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import kotlinx.coroutines.flow.Flow

fun EntitySpecProcessor.getTypeSpec(typeReference: KSTypeReference): DaoReturnTypeSpec? {
    val type = typeReference.resolve()
    val classDeclaration = type.declaration as KSClassDeclaration
    val isNullable = type.isMarkedNullable

    val specDataType = when (classDeclaration.qualifiedName?.asString()) {
        Unit::class.qualifiedName,
        null -> return null

        Flow::class.qualifiedName -> {
            val wrappedDeclaration = getTypeSpec(type.arguments.first())
            DaoReturnTypeSpec.DataType.Stream(wrappedDeclaration)
        }

        List::class.qualifiedName,
        Collection::class.qualifiedName,
        Iterable::class.qualifiedName -> {
            val wrappedDeclaration = getTypeSpec(type.arguments.first())
            DaoReturnTypeSpec.DataType.Collection(wrappedDeclaration)
        }

        else -> getSpecType(classDeclaration)
    }

    return DaoReturnTypeSpec(
        typeReference,
        classDeclaration,
        isNullable,
        specDataType
    )
}

fun EntitySpecProcessor.getTypeSpec(typeArgument: KSTypeArgument): DaoReturnTypeSpec {
    val type = requireNotNull(typeArgument.type)
    val typeDeclaration = getTypeSpec(type)
    return requireNotNull(typeDeclaration)
}

fun EntitySpecProcessor.getSpecType(
    classDeclaration: KSClassDeclaration
): DaoReturnTypeSpec.DataType {
    if (!hasEntityAnnotation(classDeclaration)) {
        return DaoReturnTypeSpec.DataType.Class
    }

    val entitySpec = getEntitySpec(classDeclaration)
    return DaoReturnTypeSpec.DataType.Entity(entitySpec)
}
