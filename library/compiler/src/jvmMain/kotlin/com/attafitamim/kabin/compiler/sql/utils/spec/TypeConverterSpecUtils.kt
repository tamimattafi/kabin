package com.attafitamim.kabin.compiler.sql.utils.spec

import app.cash.sqldelight.ColumnAdapter
import com.attafitamim.kabin.compiler.sql.generator.references.ColumnAdapterReference
import com.attafitamim.kabin.core.converters.FloatDoubleConverter
import com.attafitamim.kabin.core.converters.IntLongConverter
import com.attafitamim.kabin.specs.converters.TypeConverterSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName

val defaultAdapters = mapOf(
    FloatDoubleConverter.pairWithReference(),
    IntLongConverter.pairWithReference()
)

fun List<TypeConverterSpec>.associateByReferences() = associateBy { typeConverterSpec ->
    ColumnAdapterReference(
        typeConverterSpec.affinityType.toClassName(),
        typeConverterSpec.kotlinType.toClassName()
    )
}

inline fun <reified T : Any, reified R : Any> ColumnAdapter<T, R>.pairWithReference() =
    ColumnAdapterReference(
        R::class.asClassName(),
        T::class.asClassName()
    ) to this::class.asClassName()