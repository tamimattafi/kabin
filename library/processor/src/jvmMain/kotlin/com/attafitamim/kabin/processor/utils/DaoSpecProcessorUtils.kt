package com.attafitamim.kabin.processor.utils

import com.attafitamim.kabin.processor.spec.EntitySpecProcessor
import com.attafitamim.kabin.specs.dao.DataTypeSpec
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeArgument
import com.google.devtools.ksp.symbol.KSTypeReference
import kotlinx.coroutines.flow.Flow

fun EntitySpecProcessor.getReturnTypeSpec(typeReference: KSTypeReference): DataTypeSpec? {
    val type = typeReference.resolve()
    val classDeclaration = type.declaration as KSClassDeclaration
    val isNullable = type.isMarkedNullable

    val specDataType = when (classDeclaration.qualifiedName?.asString()) {
        Unit::class.qualifiedName,
        null -> return null

        Flow::class.qualifiedName -> {
            val wrappedDeclaration = getReturnTypeSpec(type.arguments.first())
            DataTypeSpec.DataType.Stream(wrappedDeclaration)
        }

        List::class.qualifiedName,
        Collection::class.qualifiedName,
        Iterable::class.qualifiedName -> {
            val wrappedDeclaration = getReturnTypeSpec(type.arguments.first())
            DataTypeSpec.DataType.Collection(wrappedDeclaration)
        }

        else -> getSpecType(classDeclaration)
    }

    return DataTypeSpec(
        typeReference,
        classDeclaration,
        isNullable,
        specDataType
    )
}

fun EntitySpecProcessor.getReturnTypeSpec(typeArgument: KSTypeArgument): DataTypeSpec {
    val type = requireNotNull(typeArgument.type)
    val typeDeclaration = getReturnTypeSpec(type)
    return requireNotNull(typeDeclaration)
}

fun EntitySpecProcessor.getSpecType(
    classDeclaration: KSClassDeclaration
): DataTypeSpec.DataType {
    if (!hasEntityAnnotation(classDeclaration)) {
        return DataTypeSpec.DataType.Class
    }

    val entitySpec = getEntitySpec(classDeclaration)
    return DataTypeSpec.DataType.Entity(entitySpec)
}
