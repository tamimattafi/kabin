package com.attafitamim.kabin.compiler.sql.generator.mapper

import app.cash.sqldelight.ColumnAdapter
import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.compiler.sql.generator.references.ColumnAdapterReference
import com.attafitamim.kabin.compiler.sql.utils.poet.buildSpec
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.getAdapterReference
import com.attafitamim.kabin.compiler.sql.utils.poet.entity.getParseFunction
import com.attafitamim.kabin.compiler.sql.utils.poet.entity.supportedParsers
import com.attafitamim.kabin.compiler.sql.utils.poet.references.getPropertyName
import com.attafitamim.kabin.compiler.sql.utils.poet.simpleNameString
import com.attafitamim.kabin.compiler.sql.utils.poet.toPascalCase
import com.attafitamim.kabin.compiler.sql.utils.poet.toSimpleTypeName
import com.attafitamim.kabin.compiler.sql.utils.poet.writeType
import com.attafitamim.kabin.compiler.sql.utils.spec.getMapperClassName
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.getFlatColumns
import com.attafitamim.kabin.compiler.sql.utils.sql.sqlType
import com.attafitamim.kabin.core.table.KabinMapper
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.column.ColumnTypeSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
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
        val superInterface = KabinMapper::class.asClassName()
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

        return Result(entityClassName, className, adapters)
    }


    private fun TypeSpec.Builder.addEntityParseFunction(
        entitySpec: EntitySpec
    ): Set<ColumnAdapterReference> {
        val entityClassName = entitySpec.declaration.toClassName()
        val adapters = HashSet<ColumnAdapterReference>()
        val builder = KabinMapper<*>::map.buildSpec()
            .addModifiers(KModifier.OVERRIDE)
            .returns(entityClassName)

        val codeBlockBuilder = CodeBlock.builder()

        val parameterAdapters = codeBlockBuilder
            .addEntityPropertyParsing(entitySpec.columns)

        adapters.addAll(parameterAdapters)

        val initializationLogic = addEntityInitialization(
            entitySpec.declaration,
            entitySpec.columns
        )

        codeBlockBuilder.add(initializationLogic)
        builder.addCode(codeBlockBuilder.build())

        val funSpec = builder.build()
        addFunction(funSpec)
        return adapters
    }

    private fun CodeBlock.Builder.addEntityPropertyParsing(
        columns: List<ColumnSpec>,
        parent: String? = null,
        initialIndex: Int = 0
    ): Set<ColumnAdapterReference> {
        val adapters = HashSet<ColumnAdapterReference>()
        var currentIndex = initialIndex
        columns.forEach { column ->
            val propertyName = getPropertyName(column, parent)

            when (val dataType = column.typeSpec.dataType) {
                is ColumnTypeSpec.DataType.Class -> {
                    val adapter = addPropertyParsing(
                        propertyName,
                        currentIndex,
                        column.typeAffinity,
                        column.declaration.type.resolve()
                    )

                    if (adapter != null) {
                        adapters.add(adapter)
                    }

                    currentIndex++
                }

                is ColumnTypeSpec.DataType.Embedded -> {
                    val requiredAdapters = addEntityPropertyParsing(
                        dataType.columns,
                        propertyName,
                        currentIndex
                    )

                    val requiredColumn = dataType.columns.firstOrNull { embeddedColumn ->
                        !embeddedColumn.typeSpec.type.isMarkedNullable
                    }

                    add("val·$propertyName·=·")
                    if (requiredColumn != null) {
                        val columnName = getPropertyName(requiredColumn, propertyName)
                        beginControlFlow("$columnName?.let·{")
                    }

                    val initializationLogic = addEntityInitialization(
                        column.typeSpec.declaration,
                        dataType.columns,
                        propertyName,
                        propertyName
                    )

                    add(initializationLogic)

                    if (requiredColumn != null) {
                        endControlFlow()
                    }

                    adapters.addAll(requiredAdapters)
                    currentIndex += getFlatColumns(dataType.columns).size
                }
            }
        }

        return adapters
    }

    private fun addEntityInitialization(
        declaration: KSClassDeclaration,
        columns: List<ColumnSpec>,
        parent: String? = null,
        property: String? = null
    ): CodeBlock = CodeBlock.builder().apply {
        val className = declaration.toClassName()
        if (property.isNullOrBlank()) {
            addStatement("return·%T(", className)
        } else {
            addStatement("%T(", className)
        }

        columns.forEach { column ->
            val propertyName = getPropertyName(column, parent)
            when (column.typeSpec.dataType) {
                is ColumnTypeSpec.DataType.Class -> {
                    addPropertyDecoding(
                        column.typeSpec.type.isMarkedNullable,
                        propertyName,
                        column.typeAffinity,
                        column.declaration.type.resolve()
                    )
                }

                is ColumnTypeSpec.DataType.Embedded -> {
                    if (column.typeSpec.type.isMarkedNullable) {
                        addStatement("$propertyName,")
                    } else {
                        addStatement("$propertyName!!,")
                    }
                }
            }
        }

        addStatement(")")
    }.build()

    private fun CodeBlock.Builder.addPropertyParsing(
        propertyAccess: String,
        index: Int,
        typeAffinity: ColumnInfo.TypeAffinity?,
        typeDeclaration: KSType
    ): ColumnAdapterReference? {
        val declarationAffinity = typeDeclaration.sqlType
        val actualTypeAffinity = typeAffinity ?: declarationAffinity
        val adapter = typeDeclaration.getAdapterReference(typeAffinity)

        val parseFunction = if (adapter != null) {
            actualTypeAffinity.getParseFunction()
        } else {
            supportedParsers.getValue(typeDeclaration.toSimpleTypeName())
        }

        addStatement("val·$propertyAccess·=·cursor.$parseFunction($index)")
        return adapter
    }

    private fun CodeBlock.Builder.addPropertyDecoding(
        isNullable: Boolean,
        property: String,
        typeAffinity: ColumnInfo.TypeAffinity?,
        type: KSType
    ) {
        val adapter = type.getAdapterReference(typeAffinity)

        val propertySign = if (isNullable) "?" else "!!"
        val propertyAccessor = "$property$propertySign"

        val decodedProperty = if (adapter != null) {
            val adapterName = adapter.getPropertyName()
            val decodeMethod = ColumnAdapter<*, *>::decode.name
            "$propertyAccessor.let($adapterName::$decodeMethod)"
        } else {
            if (isNullable) {
                property
            } else {
                propertyAccessor
            }
        }

        addStatement("$decodedProperty,")
    }

    private fun getPropertyName(
        columnSpec: ColumnSpec,
        parent: String?
    ): String {
        val propertyName = columnSpec.declaration.simpleNameString
        return if (parent.isNullOrBlank()) {
            propertyName
        } else {
            "$parent${propertyName.toPascalCase()}"
        }
    }

    data class Result(
        val returnType: ClassName,
        val className: ClassName,
        val adapters: Set<ColumnAdapterReference>
    )
}
