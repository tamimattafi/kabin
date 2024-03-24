package com.attafitamim.kabin.compiler.sql.utils.spec

import com.attafitamim.kabin.compiler.sql.generator.mapper.MapperGenerator
import com.attafitamim.kabin.compiler.sql.generator.references.MapperReference
import com.attafitamim.kabin.core.converters.mappers.legacy.BooleanMapper
import com.attafitamim.kabin.core.converters.mappers.legacy.BytesMapper
import com.attafitamim.kabin.core.converters.mappers.legacy.DoubleMapper
import com.attafitamim.kabin.core.converters.mappers.legacy.FloatMapper
import com.attafitamim.kabin.core.converters.mappers.legacy.IntMapper
import com.attafitamim.kabin.core.converters.mappers.legacy.LongMapper
import com.attafitamim.kabin.core.converters.mappers.legacy.StringMapper
import com.attafitamim.kabin.core.table.KabinMapper
import com.attafitamim.kabin.specs.converters.MapperSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName

val defaultMappers = mapOf(
    BooleanMapper.pairWithReference(),
    BytesMapper.pairWithReference(),
    DoubleMapper.pairWithReference(),
    LongMapper.pairWithReference(),
    StringMapper.pairWithReference(),
    IntMapper.pairWithReference(),
    FloatMapper.pairWithReference()
)

fun Collection<MapperSpec>.mapperSpecsByReferences() = associateBy { mapperSpec ->
    MapperReference(mapperSpec.returnType.toTypeName())
}

fun Collection<MapperGenerator.Result>.mapperResultByReferences() = associateBy { generatedMapper ->
    MapperReference(generatedMapper.returnType)
}

inline fun <reified T : Any> KabinMapper<T>.pairWithReference() =
    MapperReference(T::class.asTypeName()) to this::class.asClassName()
