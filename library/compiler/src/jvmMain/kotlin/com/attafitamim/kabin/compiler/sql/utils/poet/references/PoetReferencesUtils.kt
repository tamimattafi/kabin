package com.attafitamim.kabin.compiler.sql.utils.poet.references

import com.attafitamim.kabin.compiler.sql.generator.references.ColumnAdapterReference
import com.attafitamim.kabin.compiler.sql.generator.references.MapperReference
import com.attafitamim.kabin.compiler.sql.utils.poet.asPropertyName
import com.attafitamim.kabin.compiler.sql.utils.poet.toCamelCase
import com.attafitamim.kabin.compiler.sql.utils.poet.toLowerCamelCase
import com.attafitamim.kabin.compiler.sql.utils.spec.getPrefixedClassName
import com.attafitamim.kabin.core.table.KabinEntityMapper
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.squareup.kotlinpoet.asClassName

fun ColumnAdapterReference.getPropertyName() = buildString {
    append(
        kotlinType.simpleName.toLowerCamelCase(),
        affinityType.simpleName
    )
}

fun MapperReference.getPropertyName(options: KabinOptions) = entityType
    .getPrefixedClassName(options, KabinOptions.Key.ENTITY_MAPPER_SUFFIX)
    .asPropertyName()
