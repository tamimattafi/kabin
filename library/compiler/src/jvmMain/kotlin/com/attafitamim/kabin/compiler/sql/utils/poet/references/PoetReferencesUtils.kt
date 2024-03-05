package com.attafitamim.kabin.compiler.sql.utils.poet.references

import com.attafitamim.kabin.compiler.sql.generator.references.ColumnAdapterReference
import com.attafitamim.kabin.compiler.sql.generator.references.MapperReference
import com.attafitamim.kabin.compiler.sql.utils.poet.asClassName
import com.attafitamim.kabin.compiler.sql.utils.poet.asPropertyName
import com.attafitamim.kabin.compiler.sql.utils.poet.toCamelCase
import com.attafitamim.kabin.compiler.sql.utils.spec.getPrefixedClassName
import com.attafitamim.kabin.processor.ksp.options.KabinOptions

fun ColumnAdapterReference.getPropertyName() = buildString {
    append(
        kotlinType.asPropertyName(),
        affinityType.asClassName()
    )
}

fun MapperReference.getPropertyName(options: KabinOptions) = returnType
    .getPrefixedClassName(options, KabinOptions.Key.ENTITY_MAPPER_SUFFIX)
    .asPropertyName()
