package com.attafitamim.kabin.compiler.sql.utils.poet.entity

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlCursor
import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.compiler.sql.generator.references.ColumnAdapterReference
import com.attafitamim.kabin.compiler.sql.utils.poet.references.getPropertyName
import com.attafitamim.kabin.compiler.sql.utils.poet.buildSpec
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.getAdapterReference
import com.attafitamim.kabin.compiler.sql.utils.poet.toPascalCase
import com.attafitamim.kabin.compiler.sql.utils.sql.sqlType
import com.attafitamim.kabin.core.table.KabinMapper
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.column.ColumnTypeSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

val supportedAffinity: Map<ColumnInfo.TypeAffinity, TypeName> = mapOf(
    ColumnInfo.TypeAffinity.INTEGER to Long::class.asClassName(),
    ColumnInfo.TypeAffinity.TEXT to String::class.asClassName(),
    ColumnInfo.TypeAffinity.NONE to ByteArray::class.asClassName(),
    ColumnInfo.TypeAffinity.REAL to Double::class.asClassName()
)

val supportedParsers: Map<TypeName, String> = mapOf(
    Long::class.asClassName() to SqlCursor::getLong.name,
    Double::class.asClassName() to SqlCursor::getDouble.name,
    String::class.asClassName() to SqlCursor::getString.name,
    ByteArray::class.asClassName() to SqlCursor::getBytes.name,
    Boolean::class.asClassName() to SqlCursor::getBoolean.name
)

fun ColumnInfo.TypeAffinity.getParseFunction(): String = when (this) {
    ColumnInfo.TypeAffinity.INTEGER -> SqlCursor::getLong.name
    ColumnInfo.TypeAffinity.NUMERIC -> SqlCursor::getString.name
    ColumnInfo.TypeAffinity.REAL -> SqlCursor::getDouble.name
    ColumnInfo.TypeAffinity.TEXT -> SqlCursor::getString.name
    ColumnInfo.TypeAffinity.NONE -> SqlCursor::getBytes.name
    ColumnInfo.TypeAffinity.UNDEFINED -> error("Can't find parse function for this type $this")
}

fun TypeSpec.Builder.addEntityParseFunction(
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

    codeBlockBuilder.addEntityInitialization(entitySpec.declaration, entitySpec.columns)

    builder.addCode(codeBlockBuilder.build())

    val funSpec = builder.build()
    addFunction(funSpec)
    return adapters
}

fun CodeBlock.Builder.addEntityPropertyParsing(
    columns: List<ColumnSpec>,
    parent: String? = null,
    initialIndex: Int = 0
): Set<ColumnAdapterReference> {
    val adapters = HashSet<ColumnAdapterReference>()
    var currentIndex = initialIndex
    columns.forEach { column ->
        val propertyName = column.declaration.simpleName.asString()
        val propertyAccess = if (parent.isNullOrBlank()) {
            propertyName
        } else {
            "${parent}${propertyName.toPascalCase()}"
        }

        when (val dataType = column.typeSpec.dataType) {
            is ColumnTypeSpec.DataType.Class -> {
                val adapter = addPropertyParsing(
                    propertyAccess,
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
                    propertyAccess,
                    currentIndex
                )

                adapters.addAll(requiredAdapters)
                currentIndex += dataType.columns.size
            }
        }
    }

    return adapters
}

fun CodeBlock.Builder.addEntityInitialization(
    declaration: KSClassDeclaration,
    columns: List<ColumnSpec>,
    parent: String? = null
) {
    val className = declaration.toClassName()
    if (!parent.isNullOrBlank()) {
        addStatement("%T(", className)
    } else {
        addStatement("return %T(", className)
    }

    columns.forEach { column ->
        val propertyName = column.declaration.simpleName.asString()
        val propertyAccess = if (parent.isNullOrBlank()) {
            propertyName
        } else {
            "${parent}${propertyName.toPascalCase()}"
        }

        when (val dataType = column.typeSpec.dataType) {
            is ColumnTypeSpec.DataType.Class -> {
                addPropertyDecoding(
                    column.typeSpec.isNullable,
                    propertyAccess,
                    column.typeAffinity,
                    column.declaration.type.resolve()
                )
            }

            is ColumnTypeSpec.DataType.Embedded -> {
                addEntityInitialization(
                    column.typeSpec.declaration,
                    dataType.columns,
                    propertyAccess
                )
            }
        }
    }

    if (parent.isNullOrBlank()) {
        addStatement(")")
    } else {
        addStatement("),")
    }
}

fun CodeBlock.Builder.addPropertyParsing(
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
        supportedParsers.getValue(typeDeclaration.toTypeName())
    }

    addStatement("val $propertyAccess = cursor.$parseFunction($index)")
    return adapter
}

fun CodeBlock.Builder.addPropertyDecoding(
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
