package com.attafitamim.kabin.processor.utils

import com.attafitamim.kabin.annotations.entity.Embedded
import com.attafitamim.kabin.annotations.relation.Junction
import com.attafitamim.kabin.annotations.relation.Relation
import com.attafitamim.kabin.processor.spec.EntitySpecProcessor
import com.attafitamim.kabin.specs.dao.DataTypeSpec
import com.attafitamim.kabin.specs.relation.JunctionSpec
import com.attafitamim.kabin.specs.relation.RelationSpec
import com.attafitamim.kabin.specs.relation.compound.CompoundPropertySpec
import com.attafitamim.kabin.specs.relation.compound.CompoundRelationSpec
import com.attafitamim.kabin.specs.relation.compound.CompoundSpec
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.symbol.KSAnnotation
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
        type,
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
        return checkForCompound(classDeclaration)
    }

    val entitySpec = getEntitySpec(classDeclaration)
    return DataTypeSpec.DataType.Entity(entitySpec)
}

fun EntitySpecProcessor.getJunctionSpec(annotation: KSAnnotation) = with(annotation.argumentsMap) {
    val entityDeclaration = requireClassDeclaration(Junction::value.name)
    val entitySpec = getEntitySpec(entityDeclaration)

    val parentColumn = requireArgument<String>(Junction::parentColumn.name)
    val entityColumn = requireArgument<String>(Junction::entityColumn.name)

    JunctionSpec(
        entitySpec,
        parentColumn,
        entityColumn
    )
}

fun EntitySpecProcessor.checkForCompound(
    classDeclaration: KSClassDeclaration
): DataTypeSpec.DataType {
    val embedded = ArrayList<CompoundPropertySpec>()
    val relations = ArrayList<CompoundRelationSpec>()

    classDeclaration.getDeclaredProperties().forEach { propertyDeclaration ->
        val dataTypeSpec = requireNotNull(getReturnTypeSpec(propertyDeclaration.type))

        val propertySpec = CompoundPropertySpec(
            propertyDeclaration,
            dataTypeSpec
        )

        val embeddedArguments = propertyDeclaration.getAnnotationArgumentsMap(Embedded::class)
        if (embeddedArguments != null) {
            embedded.add(propertySpec)
            return@forEach
        }

        val relationArguments = propertyDeclaration.getAnnotationArgumentsMap(Relation::class)
        if (relationArguments != null) {
            val entityDeclaration = relationArguments.getClassDeclaration(Relation::entity.name)

            val junctionAnnotation = relationArguments
                .getArgument<KSAnnotation>(Relation::associateBy.name)
                ?.let(::getJunctionSpec)

            val entitySpec = entityDeclaration?.let(::getEntitySpec)
            val relationSpec = RelationSpec(
                entitySpec,
                relationArguments.requireArgument(Relation::parentColumn.name),
                relationArguments.requireArgument(Relation::entityColumn.name),
                junctionAnnotation
            )

            val compoundRelationSpec = CompoundRelationSpec(
                propertySpec,
                relationSpec
            )

            relations.add(compoundRelationSpec)
        }
    }

    return if (embedded.isEmpty()) {
        DataTypeSpec.DataType.Class
    } else {
        val compoundSpec = CompoundSpec(
            classDeclaration,
            embedded.first(),
            relations
        )

        DataTypeSpec.DataType.Compound(compoundSpec)
    }
}