package com.attafitamim.kabin.processor.spec

import app.cash.sqldelight.ColumnAdapter
import com.attafitamim.kabin.annotations.converters.TypeConverters
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.converters.TypeConverterSpec
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

class TypeConverterSpecProcessor(private val logger: KSPLogger) {

    private val typeConvertersAnnotation = TypeConverters::class

    fun getTypeConverterSpec(classDeclaration: KSClassDeclaration): TypeConverterSpec {
        val columnAdapterType = getAdapterType(classDeclaration)

        val kotlinType = columnAdapterType.arguments.first().type?.resolve()
        requireNotNull(kotlinType)

        val affinityType = columnAdapterType.arguments.last().type?.resolve()
        requireNotNull(affinityType)

        return TypeConverterSpec(
            classDeclaration,
            affinityType,
            kotlinType
        )
    }

    private fun getAdapterType(classDeclaration: KSClassDeclaration): KSType {
        val adapterInterfaceName = requireNotNull(ColumnAdapter::class.qualifiedName)

        classDeclaration.superTypes.forEach { typeReference ->
            val type = typeReference.resolve()
            val declaration = type.declaration
            if (adapterInterfaceName == requireNotNull(declaration.qualifiedName?.asString())) {
                return type
            }
        }

        logger.throwException(
            "DataType Converters should implement $adapterInterfaceName",
            classDeclaration
        )
    }
}
