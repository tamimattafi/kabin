package com.attafitamim.kabin.compiler.sql.utils.spec

import app.cash.sqldelight.ColumnAdapter
import com.attafitamim.kabin.compiler.sql.generator.references.ColumnAdapterReference
import com.attafitamim.kabin.core.converters.adapters.FloatDoubleAdapter
import com.attafitamim.kabin.core.converters.adapters.IntLongAdapter
import com.attafitamim.kabin.processor.utils.classDeclaration
import com.attafitamim.kabin.specs.converters.TypeConverterSpec
import com.google.devtools.ksp.symbol.ClassKind
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName

val defaultAdapters = mapOf(
    FloatDoubleAdapter.pairWithReference(),
    IntLongAdapter.pairWithReference()
)

fun Collection<TypeConverterSpec>.converterSpecsByReferences() = associateBy { typeConverterSpec ->
    ColumnAdapterReference(
        typeConverterSpec.affinityType.toClassName(),
        typeConverterSpec.kotlinType.toClassName(),
        typeConverterSpec.kotlinType.classDeclaration.classKind
    )
}

inline fun <reified T : Any, reified R : Any> ColumnAdapter<T, R>.pairWithReference() =
    ColumnAdapterReference(
        R::class.asClassName(),
        T::class.asClassName(),
        ClassKind.CLASS
    ) to this::class.asClassName()