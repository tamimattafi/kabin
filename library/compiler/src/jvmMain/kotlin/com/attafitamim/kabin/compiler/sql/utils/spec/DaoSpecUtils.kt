package com.attafitamim.kabin.compiler.sql.utils.spec

import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.specs.dao.DaoSpec
import com.attafitamim.kabin.specs.dao.DataTypeSpec
import com.squareup.kotlinpoet.ClassName

fun DaoSpec.getQueryClassName(options: KabinOptions): ClassName =
    declaration.getClassName(options, KabinOptions.Key.DAO_QUERIES_SUFFIX)

fun DaoSpec.getDaoClassName(options: KabinOptions): ClassName =
    declaration.getClassName(options, KabinOptions.Key.DAO_SUFFIX)

fun DataTypeSpec.getDataReturnType(): DataTypeSpec {
    var currentType = this
    var currentTypeDataType = currentType.dataType

    while (currentTypeDataType is DataTypeSpec.DataType.Wrapper) {
        currentType = currentTypeDataType.wrappedDeclaration
        currentTypeDataType = currentType.dataType
    }

    return currentType
}
