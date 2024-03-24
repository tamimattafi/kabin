package com.attafitamim.kabin.compiler.sql.utils.spec

import app.cash.sqldelight.ColumnAdapter
import com.attafitamim.kabin.compiler.sql.generator.references.ColumnAdapterReference
import com.attafitamim.kabin.compiler.sql.utils.poet.toSimpleTypeName
import com.attafitamim.kabin.core.converters.adapters.integer.ByteLongAdapter
import com.attafitamim.kabin.core.converters.adapters.text.DoubleStringAdapter
import com.attafitamim.kabin.core.converters.adapters.real.FloatDoubleAdapter
import com.attafitamim.kabin.core.converters.adapters.text.FloatStringAdapter
import com.attafitamim.kabin.core.converters.adapters.integer.IntLongAdapter
import com.attafitamim.kabin.core.converters.adapters.integer.LongIntAdapter
import com.attafitamim.kabin.core.converters.adapters.text.IntStringAdapter
import com.attafitamim.kabin.core.converters.adapters.text.LongStringAdapter
import com.attafitamim.kabin.core.converters.adapters.integer.ShortLongAdapter
import com.attafitamim.kabin.core.converters.adapters.real.DoubleFloatAdapter
import com.attafitamim.kabin.core.converters.adapters.text.StringDoubleAdapter
import com.attafitamim.kabin.core.converters.adapters.text.StringFloatAdapter
import com.attafitamim.kabin.core.converters.adapters.text.StringIntAdapter
import com.attafitamim.kabin.core.converters.adapters.text.StringLongAdapter
import com.attafitamim.kabin.processor.utils.classDeclaration
import com.attafitamim.kabin.specs.converters.TypeConverterSpec
import com.google.devtools.ksp.symbol.ClassKind
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

val defaultAdapters = mapOf(
    ByteLongAdapter.pairWithReference(),
    DoubleStringAdapter.pairWithReference(),
    StringDoubleAdapter.pairWithReference(),
    FloatDoubleAdapter.pairWithReference(),
    DoubleFloatAdapter.pairWithReference(),
    FloatStringAdapter.pairWithReference(),
    StringFloatAdapter.pairWithReference(),
    IntLongAdapter.pairWithReference(),
    LongIntAdapter.pairWithReference(),
    IntStringAdapter.pairWithReference(),
    StringIntAdapter.pairWithReference(),
    LongStringAdapter.pairWithReference(),
    StringLongAdapter.pairWithReference(),
    ShortLongAdapter.pairWithReference()
)

fun Collection<TypeConverterSpec>.converterSpecsByReferences() = associateBy { typeConverterSpec ->
    ColumnAdapterReference(
        typeConverterSpec.affinityType.toSimpleTypeName(),
        typeConverterSpec.kotlinType.toSimpleTypeName(),
        typeConverterSpec.kotlinType.classDeclaration.classKind
    )
}

inline fun <reified T : Any, reified R : Any> ColumnAdapter<T, R>.pairWithReference() =
    ColumnAdapterReference(
        R::class.asTypeName().copy(false),
        T::class.asTypeName().copy(false),
        ClassKind.CLASS
    ) to this::class.asClassName()