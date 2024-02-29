package com.attafitamim.kabin.compiler.sql.generator.mapper

import app.cash.sqldelight.ColumnAdapter
import com.attafitamim.kabin.compiler.sql.generator.references.ColumnAdapterReference
import com.attafitamim.kabin.compiler.sql.utils.poet.entity.addEntityParseFunction
import com.attafitamim.kabin.compiler.sql.utils.poet.references.getPropertyName
import com.attafitamim.kabin.compiler.sql.utils.poet.writeType
import com.attafitamim.kabin.compiler.sql.utils.spec.getMapperClassName
import com.attafitamim.kabin.compiler.sql.utils.spec.getTableClassName
import com.attafitamim.kabin.core.table.KabinEntityMapper
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName

class MapperGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: KabinOptions
) {

    fun generate(entitySpec: EntitySpec): Result {
        val className = entitySpec.getMapperClassName(options)

        val entityClassName = entitySpec.declaration.toClassName()
        val superInterface = KabinEntityMapper::class.asClassName()
            .parameterizedBy(entityClassName)

        val classBuilder = TypeSpec.classBuilder(className)
            .addSuperinterface(superInterface)


        val adapters = classBuilder.addEntityParseFunction(entitySpec)
        val constructorBuilder = FunSpec.constructorBuilder()
        adapters.forEach { adapter ->
            val propertyName = adapter.getPropertyName()
            val adapterType = ColumnAdapter::class.asClassName()
                .parameterizedBy(adapter.kotlinType, adapter.affinityType)

            val propertySpec = PropertySpec.builder(
                propertyName,
                adapterType,
                KModifier.PRIVATE
            ).initializer(propertyName).build()

            classBuilder.addProperty(propertySpec)
            constructorBuilder.addParameter(
                adapter.getPropertyName(),
                adapterType
            )
        }

        classBuilder.primaryConstructor(constructorBuilder.build())

        codeGenerator.writeType(
            className,
            classBuilder.build()
        )

        return Result(className, adapters)
    }

    data class Result(
        val className: ClassName,
        val adapters: Set<ColumnAdapterReference>
    )
}
