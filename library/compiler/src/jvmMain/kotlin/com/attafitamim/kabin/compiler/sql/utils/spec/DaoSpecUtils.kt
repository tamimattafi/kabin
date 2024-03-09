package com.attafitamim.kabin.compiler.sql.utils.spec

import com.attafitamim.kabin.compiler.sql.generator.references.ParameterReference
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
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
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

fun <T : Any> Collection<T>.toSortedSet() = LinkedHashSet(this)

fun EntitySpec.getQueryFunctionName(query: SQLQuery): String =
    when (query) {
        is SQLQuery.Columns -> getQueryByColumnsName(query.columns.toSortedSet())
        is SQLQuery.Parameters -> declaration.getQueryByParametersName(query.parameters.toSortedSet())
        is SQLQuery.Raw -> declaration.getQueryByParametersName(setOf(query.rawQueryParameter))
    }

fun EntitySpec.getQueryByColumnsName(columns: Set<ColumnSpec>): String =
    if (columns.isEmpty()) {
        declaration.getQueryByNoParametersName()
    } else declaration.buildQueryFunctionName {
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

fun EntitySpec.getQueryByColumnsName(column: ColumnSpec): String =
    declaration.buildQueryFunctionName {
        val columnsAccess = getColumnAccessChain(column.name)
        columnsAccess.forEach { access ->
            if (access.typeSpec.isNullable) {
                append("Optional")
            }

            append(access.declaration.simpleNameString.toPascalCase())
        }
    }

fun KSClassDeclaration.getQueryByColumnsName(columns: Set<ColumnSpec>): String =
    if (columns.isEmpty()) {
        getQueryByNoParametersName()
    } else buildQueryFunctionName {
        columns.forEach { columnSpec ->
            if (columnSpec.typeSpec.isNullable) {
                append("Optional")
            }

            append(columnSpec.declaration.simpleNameString.toPascalCase())
        }
    }

fun KSClassDeclaration.getQueryByParametersName(parameters: Set<DaoParameterSpec>): String =
    if (parameters.isEmpty()) {
        getQueryByNoParametersName()
    } else buildQueryFunctionName {
        parameters.forEach { parameter ->
            if (parameter.typeSpec.isNullable) {
                append("Optional")
            }

            append(parameter.name.toPascalCase())
        }
    }

fun KSClassDeclaration.getQueryByNoParametersName(): String =
    buildQueryFunctionName {
        append("NoParameters")
    }

fun KSClassDeclaration.buildQueryFunctionName(builder: StringBuilder.() -> Unit): String =
    buildString {
        append("query")
        append(simpleNameString.toPascalCase())
        append("By")

        builder()
    }
