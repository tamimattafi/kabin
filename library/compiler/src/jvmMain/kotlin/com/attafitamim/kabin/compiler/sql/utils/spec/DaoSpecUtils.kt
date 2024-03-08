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

inline fun <reified T : DataTypeSpec.DataType.Data> DataTypeSpec.getSpecificNestedDataType(): T {
    val currentType = getNestedDataType()
    return currentType.dataType as T
}

fun DataTypeSpec.getEntityDataType(): DataTypeSpec.DataType.Entity {
    var currentDataType = getNestedDataType().dataType
    while (currentDataType !is DataTypeSpec.DataType.Entity) {
        val compoundType = currentDataType as DataTypeSpec.DataType.Compound
        currentDataType = compoundType.compoundSpec.mainProperty.dataTypeSpec.dataType
    }

    return currentDataType
}

fun EntitySpec.getQueryFunctionName(query: SQLQuery): String =
    when (query) {
        is SQLQuery.Columns -> getQueryByColumnsName(query.columns)
        is SQLQuery.Parameters -> getQueryByParametersName(query.parameters)
        is SQLQuery.Raw -> getQueryByParametersName(listOf(query.rawQueryParameter))
    }

fun EntitySpec.getQueryByColumnsName(columns: Collection<ColumnSpec>): String =
    if (columns.isEmpty()) {
        getQueryByNoParametersName()
    } else buildQueryFunctionName {
        columns.forEach { columnSpec ->
            val columnsAccess = getColumnAccessChain(columnSpec.name)
            columnsAccess.forEach { access ->
                append(access.declaration.simpleNameString.toPascalCase())
            }
        }
    }

fun EntitySpec.getQueryByParametersName(parameters: Collection<DaoParameterSpec>): String =
    if (parameters.isEmpty()) {
        getQueryByNoParametersName()
    } else buildQueryFunctionName {
        parameters.forEach { parameter ->
            append(parameter.name.toPascalCase())
        }
    }

fun EntitySpec.getQueryByNoParametersName(): String =
    buildQueryFunctionName {
        append("NoParameters")
    }

fun EntitySpec.getQueryByRawName(): String =
    buildQueryFunctionName {
        append("RawQuery")
    }

fun EntitySpec.buildQueryFunctionName(builder: StringBuilder.() -> Unit): String =
    buildString {
        append("query")
        append(declaration.simpleNameString.toPascalCase())
        append("By")

        builder()
    }
