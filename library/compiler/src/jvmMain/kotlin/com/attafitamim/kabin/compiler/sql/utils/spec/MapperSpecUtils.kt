package com.attafitamim.kabin.compiler.sql.utils.spec

import com.attafitamim.kabin.compiler.sql.generator.mapper.MapperGenerator
import com.attafitamim.kabin.compiler.sql.generator.references.MapperReference
import com.attafitamim.kabin.core.converters.mappers.BooleanMapper
import com.attafitamim.kabin.core.converters.mappers.BytesMapper
import com.attafitamim.kabin.core.converters.mappers.DoubleMapper
import com.attafitamim.kabin.core.converters.mappers.FloatMapper
import com.attafitamim.kabin.core.converters.mappers.IntMapper
import com.attafitamim.kabin.core.converters.mappers.LongMapper
import com.attafitamim.kabin.core.converters.mappers.StringMapper
import com.attafitamim.kabin.core.table.KabinMapper
import com.attafitamim.kabin.specs.converters.MapperSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName

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
    MapperReference(mapperSpec.returnType.toClassName())
}

fun Collection<MapperGenerator.Result>.mapperResultByReferences() = associateBy { generatedMapper ->
    MapperReference(generatedMapper.returnType)
}

inline fun <reified T : Any> KabinMapper<T>.pairWithReference() =
    MapperReference(T::class.asClassName()) to this::class.asClassName()
