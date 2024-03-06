package com.attafitamim.kabin.compiler.sql.utils.poet.dao

import app.cash.sqldelight.db.SqlPreparedStatement
import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.compiler.sql.generator.references.ColumnAdapterReference
import com.attafitamim.kabin.compiler.sql.utils.poet.SYMBOL_ACCESS_SIGN
import com.attafitamim.kabin.compiler.sql.utils.poet.entity.supportedAffinity
import com.attafitamim.kabin.compiler.sql.utils.poet.qualifiedNameString
import com.attafitamim.kabin.compiler.sql.utils.poet.simpleNameString
import com.attafitamim.kabin.compiler.sql.utils.poet.toPascalCase
import com.attafitamim.kabin.compiler.sql.utils.sql.dao.getFlatColumns
import com.attafitamim.kabin.compiler.sql.utils.sql.sqlType
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.column.ColumnTypeSpec
import com.attafitamim.kabin.specs.dao.DaoFunctionSpec
import com.attafitamim.kabin.specs.dao.DataTypeSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.attafitamim.kabin.specs.relation.compound.CompoundPropertySpec
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName

val supportedBinders = mapOf(
    Long::class.qualifiedName to SqlPreparedStatement::bindLong.name,
    Double::class.qualifiedName to SqlPreparedStatement::bindDouble.name,
    String::class.qualifiedName to SqlPreparedStatement::bindString.name,
    ByteArray::class.qualifiedName to SqlPreparedStatement::bindBytes.name,
    Boolean::class.qualifiedName to SqlPreparedStatement::bindBoolean.name
)

val List<ColumnSpec>.isNullableAccess: Boolean get() = any { columnSpec ->
    columnSpec.typeSpec.isNullable
}

fun DaoFunctionSpec.getCompoundFunctionName(parents: Set<CompoundPropertySpec>) =
    buildString {
        append(declaration.simpleNameString)
        parents.forEach { compoundPropertySpec ->
            append(compoundPropertySpec.declaration.simpleNameString.toPascalCase())
        }
    }

fun DaoFunctionSpec.getParametersCall(): String = parameters.joinToString { parameter ->
    parameter.name
}

fun Set<CompoundPropertySpec>.asName(): String {
    val stringBuilder = StringBuilder()
    forEachIndexed { index, compoundPropertySpec ->
        val name = compoundPropertySpec.declaration.simpleNameString
        if (index == 0) {
            stringBuilder.append(name)
        } else {
            stringBuilder.append(name.toPascalCase())
        }
    }

    return stringBuilder.toString()
}

fun DataTypeSpec.getColumnParameterAccess(columnName: String): List<ColumnSpec> =
    when (val type = dataType) {
        is DataTypeSpec.DataType.Entity -> type.spec.getColumnAccessChain(columnName)

        is DataTypeSpec.DataType.Compound -> {
            type.spec.mainProperty.dataTypeSpec.getColumnParameterAccess(columnName)
        }

        is DataTypeSpec.DataType.Collection -> {
            type.wrappedDeclaration.getColumnParameterAccess(columnName)
        }

        is DataTypeSpec.DataType.Stream,
        is DataTypeSpec.DataType.Class -> error("not supported here")
    }

fun EntitySpec.getColumnParameterAccess(columnName: String): String {
    val chain = columns.getAccessChain(columnName)

    if (chain.isEmpty()) {
        error("getAccessChain with column $columnName returned empty chain from columns $this")
    }

    return buildString {
        for (columnIndex in 0 until chain.lastIndex) {
            val column = chain[columnIndex]
            append(column.declaration.simpleNameString)

            if (column.typeSpec.isNullable) {
                append("?")
            }

            append(SYMBOL_ACCESS_SIGN)
        }

        append(chain.last().declaration.simpleNameString)
    }
}

fun EntitySpec.getColumnAccessChain(columnName: String): List<ColumnSpec> {
    val chain = columns.getAccessChain(columnName)

    if (chain.isEmpty()) {
        error("getAccessChain with column $columnName returned empty chain from entity $this")
    }

    return chain
}

fun ColumnSpec.getAccessChain(columnName: String): List<ColumnSpec> {
    val chain = ArrayList<ColumnSpec>()
    when (val dataType = typeSpec.dataType) {
        is ColumnTypeSpec.DataType.Class -> {
            if (name == columnName) {
                chain.add(this)
            }
        }

        is ColumnTypeSpec.DataType.Embedded -> {
            val newChain = dataType.columns.getAccessChain(columnName)
            if (newChain.isNotEmpty()) {
                chain.add(this)
                chain.addAll(newChain)
            }
        }
    }

    return chain
}

fun List<ColumnSpec>.getAccessChain(columnName: String): List<ColumnSpec> {
    forEach { columnSpec ->
        val chain = columnSpec.getAccessChain(columnName)
        if (chain.isNotEmpty()) {
            return chain
        }
    }

    return emptyList()
}

fun KSClassDeclaration.needsConvert(
    typeAffinity: ColumnInfo.TypeAffinity?
): Boolean {
    val isSameAffinity = typeAffinity == null ||
            typeAffinity == ColumnInfo.TypeAffinity.UNDEFINED ||
            typeAffinity == sqlType

    return !isSameAffinity || !supportedBinders.containsKey(qualifiedNameString)
}

fun KSClassDeclaration.getAdapterReference(
    typeAffinity: ColumnInfo.TypeAffinity?
): ColumnAdapterReference? {
    if (!needsConvert(typeAffinity)) {
        return null
    }

    val actualAffinity = typeAffinity ?: sqlType
    val affinityType = supportedAffinity.getValue(actualAffinity).asClassName()
    return ColumnAdapterReference(
        affinityType,
        toClassName(),
        classKind
    )
}
