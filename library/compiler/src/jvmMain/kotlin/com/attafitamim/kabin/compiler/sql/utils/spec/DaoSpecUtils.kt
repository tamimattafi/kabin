package com.attafitamim.kabin.compiler.sql.utils.spec

import com.attafitamim.kabin.compiler.sql.syntax.SQLQuery
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.getColumnAccessChain
import com.attafitamim.kabin.compiler.sql.utils.poet.simpleNameString
import com.attafitamim.kabin.compiler.sql.utils.poet.toPascalCase
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.dao.DaoParameterSpec
import com.attafitamim.kabin.specs.dao.DaoSpec
import com.attafitamim.kabin.specs.dao.DataTypeSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.attafitamim.kabin.specs.relation.compound.CompoundPropertySpec
import com.attafitamim.kabin.specs.relation.compound.CompoundSpec
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterSpec
import java.lang.StringBuilder

fun DaoSpec.getQueryFunctionName(options: KabinOptions): ClassName =
    declaration.getClassName(options, KabinOptions.Key.DAO_QUERIES_SUFFIX)

fun DaoSpec.getDaoClassName(options: KabinOptions): ClassName =
    declaration.getClassName(options, KabinOptions.Key.DAO_SUFFIX)

fun DataTypeSpec.getNestedDataType(): DataTypeSpec {
    var currentType = this
    var currentTypeDataType = currentType.dataType

    while (currentTypeDataType is DataTypeSpec.DataType.Wrapper) {
        currentType = currentTypeDataType.nestedTypeSpec
        currentTypeDataType = currentType.dataType
    }

    return currentType
}

fun DataTypeSpec.getEntityDataType(): DataTypeSpec.DataType.Entity {
    var currentDataType = getNestedDataType().dataType
    while (currentDataType !is DataTypeSpec.DataType.Entity) {
        val compoundType = currentDataType as DataTypeSpec.DataType.Compound
        currentDataType = compoundType.compoundSpec.mainProperty.dataTypeSpec.dataType
    }

    return currentDataType
}

fun DataTypeSpec.findEntityDataType(): DataTypeSpec.DataType.Entity? {
    var currentDataType = getNestedDataType().dataType
    if (currentDataType is DataTypeSpec.DataType.Class) {
        return null
    }

    while (currentDataType !is DataTypeSpec.DataType.Entity) {
        val compoundType = currentDataType as DataTypeSpec.DataType.Compound
        currentDataType = compoundType.compoundSpec.mainProperty.dataTypeSpec.dataType
    }

    return currentDataType
}

fun CompoundPropertySpec.getMainEntityAccess(): List<CompoundPropertySpec> {
    val access = ArrayList<CompoundPropertySpec>()
    access.add(this)

    var currentProperty = this
    val currentDataType = currentProperty.dataTypeSpec.getNestedDataType().dataType
    while (currentDataType !is DataTypeSpec.DataType.Entity) {
        val compoundType = currentDataType as DataTypeSpec.DataType.Compound
        currentProperty = compoundType.compoundSpec.mainProperty
        access.add(currentProperty)
    }

    return access
}

fun <T : Any> Collection<T>.toSortedSet() = LinkedHashSet(this)

fun Collection<SQLQuery.Parameters.QueryParameter>.toSpecs() = map { queryParameter ->
    queryParameter.spec
}

fun EntitySpec.getQueryFunctionName(
    query: SQLQuery,
    isNullable: Boolean,
    parent: CompoundSpec?
): String =
    when (query) {
        is SQLQuery.Columns -> getQueryByColumnsName(query.columns.toSortedSet(), isNullable, parent)
        is SQLQuery.Parameters -> declaration.getQueryByParametersName(query.queryParameters.toSpecs().toSortedSet(), isNullable, parent)
        is SQLQuery.Raw -> declaration.getQueryByParametersName(setOf(query.rawQueryParameter), isNullable, parent)
    }

fun EntitySpec.getQueryByColumnsName(
    columns: Set<ColumnSpec>,
    isNullable: Boolean,
    parent: CompoundSpec?
): String =
    if (columns.isEmpty()) {
        declaration.getQueryByNoParametersName(isNullable, parent)
    } else declaration.buildQueryFunctionName(isNullable, parent) {
        columns.forEach { columnSpec ->
            val columnsAccess = getColumnAccessChain(columnSpec.name)
            columnsAccess.forEach { access ->
                if (access.typeSpec.isNullable) {
                    append("Optional")
                }

                append(access.declaration.simpleNameString.toPascalCase())
            }
        }
    }

fun EntitySpec.getQueryByColumnsName(
    column: ColumnSpec,
    isNullable: Boolean,
    parent: CompoundSpec?
): String =
    declaration.buildQueryFunctionName(isNullable, parent) {
        val columnsAccess = getColumnAccessChain(column.name)
        columnsAccess.forEach { access ->
            if (access.typeSpec.isNullable) {
                append("Optional")
            }

            append(access.declaration.simpleNameString.toPascalCase())
        }
    }

fun KSClassDeclaration.getQueryByColumnsName(
    columns: Set<ColumnSpec>,
    isNullable: Boolean,
    parent: CompoundSpec?
): String =
    if (columns.isEmpty()) {
        getQueryByNoParametersName(isNullable, parent)
    } else buildQueryFunctionName(isNullable, parent) {
        columns.forEach { columnSpec ->
            if (columnSpec.typeSpec.isNullable) {
                append("Optional")
            }

            append(columnSpec.declaration.simpleNameString.toPascalCase())
        }
    }

fun DataTypeSpec.getQueryByColumnsName(
    columns: Set<ColumnSpec>,
    parent: CompoundSpec?
): String =
    if (columns.isEmpty()) {
        declaration.getQueryByNoParametersName(isNullable, parent)
    } else declaration.buildQueryFunctionName(isNullable, parent) {
        columns.forEach { columnSpec ->
            if (columnSpec.typeSpec.isNullable) {
                append("Optional")
            }

            append(columnSpec.declaration.simpleNameString.toPascalCase())
        }
    }


fun KSClassDeclaration.getQueryByParametersName(
    parameters: Set<DaoParameterSpec>,
    isNullable: Boolean,
    parent: CompoundSpec?
): String =
    if (parameters.isEmpty()) {
        getQueryByNoParametersName(isNullable, parent)
    } else buildQueryFunctionName(isNullable, parent) {
        parameters.forEach { parameter ->
            if (parameter.typeSpec.isNullable) {
                append("Optional")
            }

            append(parameter.name.toPascalCase())
        }
    }

fun KSClassDeclaration.getQueryByNoParametersName(
    isNullable: Boolean,
    parent: CompoundSpec?
): String = buildQueryFunctionName(isNullable, parent) {
    append("NoParameters")
}

fun KSClassDeclaration.buildQueryFunctionName(
    isNullable: Boolean = false,
    parent: CompoundSpec?,
    builder: StringBuilder.() -> Unit
): String =
    buildString {
        append("query")

        if (isNullable) {
            append("Optional")
        }

        append(simpleNameString.toPascalCase())

        if (parent != null) {
            append("For")
            append(parent.declaration.simpleNameString.toPascalCase())
        }

        append("By")
        builder()
    }
