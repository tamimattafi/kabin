package com.attafitamim.kabin.compiler.sql.utils.poet.entity

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlPreparedStatement
import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.compiler.sql.generator.references.ColumnAdapterReference
import com.attafitamim.kabin.compiler.sql.utils.poet.references.getPropertyName
import com.attafitamim.kabin.compiler.sql.utils.poet.buildSpec
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.getAdapterReference
import com.attafitamim.kabin.compiler.sql.utils.poet.qualifiedNameString
import com.attafitamim.kabin.compiler.sql.utils.poet.simpleNameString
import com.attafitamim.kabin.compiler.sql.utils.sql.sqlType
import com.attafitamim.kabin.core.table.KabinMapper
import com.attafitamim.kabin.processor.utils.resolveClassDeclaration
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toClassName

val supportedAffinity = mapOf(
    ColumnInfo.TypeAffinity.INTEGER to Long::class,
    ColumnInfo.TypeAffinity.TEXT to String::class,
    ColumnInfo.TypeAffinity.NONE to ByteArray::class,
    ColumnInfo.TypeAffinity.REAL to Double::class
)

val supportedParsers = mapOf(
    Long::class.qualifiedName to SqlPreparedStatement::bindLong.name,
    Double::class.qualifiedName to SqlPreparedStatement::bindDouble.name,
    String::class.qualifiedName to SqlPreparedStatement::bindString.name,
    ByteArray::class.qualifiedName to SqlPreparedStatement::bindBytes.name,
    Boolean::class.qualifiedName to SqlPreparedStatement::bindBoolean.name
)

fun KSDeclaration.needsConvert(
    typeAffinity: ColumnInfo.TypeAffinity?
): Boolean {
    val isSameAffinity = typeAffinity == null ||
            typeAffinity == ColumnInfo.TypeAffinity.UNDEFINED ||
            typeAffinity == sqlType

    return !isSameAffinity || !supportedParsers.containsKey(qualifiedNameString)
}

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
        .addEntityPropertyParsing(entitySpec)

    adapters.addAll(parameterAdapters)

    codeBlockBuilder.addEntityInitialization(entitySpec)

    builder.addCode(codeBlockBuilder.build())

    val funSpec = builder.build()
    addFunction(funSpec)
    return adapters
}

fun CodeBlock.Builder.addEntityPropertyParsing(
    entitySpec: EntitySpec
): Set<ColumnAdapterReference> {
    val adapters = HashSet<ColumnAdapterReference>()
    entitySpec.columns.forEachIndexed { index, column ->
        val propertyName = column.declaration.simpleName.asString()

        val adapter = addPropertyParsing(
            propertyName,
            index,
            column.typeAffinity,
            column.declaration.type.resolveClassDeclaration()
        )

        if (adapter != null) {
            adapters.add(adapter)
        }
    }

    return adapters
}

fun CodeBlock.Builder.addEntityInitialization(
    entitySpec: EntitySpec
) {
    addStatement("return ${entitySpec.declaration.simpleNameString}(")
    entitySpec.columns.forEach { column ->
        val propertyName = column.declaration.simpleName.asString()

        addPropertyDecoding(
            column.declaration.type.resolve().isMarkedNullable,
            propertyName,
            column.typeAffinity,
            column.declaration.type.resolveClassDeclaration()
        )
    }
    addStatement(")")
}

fun CodeBlock.Builder.addPropertyParsing(
    property: String,
    index: Int,
    typeAffinity: ColumnInfo.TypeAffinity?,
    typeDeclaration: KSClassDeclaration
): ColumnAdapterReference? {
    val declarationAffinity = typeDeclaration.sqlType
    val actualTypeAffinity = typeAffinity ?: declarationAffinity
    val adapter = typeDeclaration.getAdapterReference(typeAffinity)
    val parseFunction = actualTypeAffinity.getParseFunction()
    addStatement("val $property = cursor.$parseFunction($index)")

    /*
    val actualParameter = if (adapter != null) {
        val adapterName = adapter.getPropertyName()
        val decodeMethod = ColumnAdapter<*, *>::decode.name
        "$adapterName.$decodeMethod($property)"
    } else {
        property
    }

*/

    return adapter
}

fun CodeBlock.Builder.addPropertyDecoding(
    isNullable: Boolean,
    property: String,
    typeAffinity: ColumnInfo.TypeAffinity?,
    typeDeclaration: KSClassDeclaration
) {
    val adapter = typeDeclaration.getAdapterReference(typeAffinity)

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
