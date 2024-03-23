package com.attafitamim.kabin.processor.spec

import com.attafitamim.kabin.annotations.Mappers
import com.attafitamim.kabin.core.table.KabinMapper
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.converters.MapperSpec
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType

class MapperSpecProcessor(private val logger: KSPLogger) {

    private val mappersAnnotation = Mappers::class

    fun getTypeConverterSpec(classDeclaration: KSClassDeclaration): MapperSpec {
        val mapperType = getMapperType(classDeclaration)

        val returnType = mapperType.arguments.first().type?.resolve()
        requireNotNull(returnType)

        return MapperSpec(
            classDeclaration,
            returnType
        )
    }

    private fun getMapperType(classDeclaration: KSClassDeclaration): KSType {
        val mapperInterfaceName = requireNotNull(KabinMapper::class.qualifiedName)

        classDeclaration.superTypes.forEach { typeReference ->
            val type = typeReference.resolve()
            val declaration = type.declaration
            if (mapperInterfaceName == requireNotNull(declaration.qualifiedName?.asString())) {
                return type
            }
        }

        logger.throwException(
            "Mappers should implement $mapperInterfaceName",
            classDeclaration
        )
    }
}
