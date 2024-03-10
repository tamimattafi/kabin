package com.attafitamim.kabin.compiler.sql.utils.poet.dao

import app.cash.sqldelight.db.SqlPreparedStatement
import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.compiler.sql.generator.references.ColumnAdapterReference
import com.attafitamim.kabin.compiler.sql.generator.references.FunctionReference
import com.attafitamim.kabin.compiler.sql.generator.references.ParameterReference
import com.attafitamim.kabin.compiler.sql.utils.poet.SYMBOL_ACCESS_SIGN
import com.attafitamim.kabin.compiler.sql.utils.poet.entity.supportedAffinity
import com.attafitamim.kabin.compiler.sql.utils.poet.simpleNameString
import com.attafitamim.kabin.compiler.sql.utils.poet.toPascalCase
import com.attafitamim.kabin.compiler.sql.utils.spec.toSortedSet
import com.attafitamim.kabin.compiler.sql.utils.sql.sqlType
import com.attafitamim.kabin.processor.utils.classDeclaration
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.column.ColumnTypeSpec
import com.attafitamim.kabin.specs.dao.DaoFunctionSpec
import com.attafitamim.kabin.specs.dao.DaoParameterSpec
import com.attafitamim.kabin.specs.dao.DataTypeSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.attafitamim.kabin.specs.relation.compound.CompoundPropertySpec
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName

val supportedBinders: Map<TypeName, String> = mapOf(
    Long::class.asClassName() to SqlPreparedStatement::bindLong.name,
    Double::class.asClassName() to SqlPreparedStatement::bindDouble.name,
    String::class.asClassName() to SqlPreparedStatement::bindString.name,
    ByteArray::class.asClassName() to SqlPreparedStatement::bindBytes.name,
    Boolean::class.asClassName() to SqlPreparedStatement::bindBoolean.name
)

fun DaoParameterSpec.toReference() = ParameterReference(
    name,
    typeSpec.type.toTypeName()
)

fun ColumnSpec.toReference() = ParameterReference(
    declaration.simpleNameString,
    typeSpec.type.toTypeName()
)

fun Collection<DaoParameterSpec>.toReferences() = toSortedSet().map(DaoParameterSpec::toReference)

fun Collection<ColumnSpec>.toParameterReferences() = toSortedSet().map(ColumnSpec::toReference)

fun DaoFunctionSpec.toReference() = FunctionReference(
    declaration.simpleNameString,
    parameters.toReferences()
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

fun Collection<ParameterReference>.getParametersCall() = joinToString(",·") { parameter ->
    parameter.name
}

fun DaoFunctionSpec.getParametersCall(): String = parameters.joinToString(",·") { parameter ->
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

fun EntitySpec.getColumnParameterAccess(columnName: String): String {
    val chain = columns.getAccessChain(columnName)

    if (chain.isEmpty()) {
        error("getColumnParameterAccess with column $columnName returned empty chain from columns $this")
    }

    return chain.toParameterAccess()
}

fun List<ColumnSpec>.toParameterAccess(): String {
    val stringBuilder = StringBuilder()
    for (columnIndex in 0 until lastIndex) {
        val column = this[columnIndex]
        stringBuilder.append(column.declaration.simpleNameString)

        if (column.typeSpec.isNullable) {
            stringBuilder.append("?")
        }

        stringBuilder.append(SYMBOL_ACCESS_SIGN)
    }

    stringBuilder.append(last().declaration.simpleNameString)
    return stringBuilder.toString()
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

fun KSType.needsConvert(
    typeAffinity: ColumnInfo.TypeAffinity?
): Boolean {
    val isSameAffinity = typeAffinity == null ||
            typeAffinity == ColumnInfo.TypeAffinity.UNDEFINED ||
            typeAffinity == sqlType

    return !isSameAffinity || !supportedBinders.containsKey(classDeclaration.toClassName())
}

fun ColumnSpec.getAdapterReference(
    otherColumnSpec: ColumnSpec
): ColumnAdapterReference? {
    val typeName = typeSpec.type.toTypeName().copy(nullable = false)
    val otherTypeName = otherColumnSpec.typeSpec.type.toTypeName().copy(nullable = false)
    if (typeName == otherTypeName) {
        return null
    }

    return ColumnAdapterReference(
        otherTypeName,
        typeName,
        typeSpec.declaration.classKind
    )
}

fun KSType.getAdapterReference(
    typeAffinity: ColumnInfo.TypeAffinity?
): ColumnAdapterReference? {
    if (!needsConvert(typeAffinity)) {
        return null
    }

    val actualAffinity = typeAffinity ?: sqlType
    val affinityType = supportedAffinity.getValue(actualAffinity)

    val adapter = ColumnAdapterReference(
        affinityType.copy(false),
        toTypeName().copy(false),
        classDeclaration.classKind
    )

    if (adapter.affinityType == adapter.kotlinType) {
        error("""
            Needs convert ${needsConvert(typeAffinity)}
            given typeAffinity: $typeAffinity
            given type: ${toTypeName()}
            sqlType: $sqlType
            supportedBinders: ${supportedBinders[classDeclaration.toClassName()]}
            adapter: $adapter
        """.trimIndent())
    }

    return adapter
}
