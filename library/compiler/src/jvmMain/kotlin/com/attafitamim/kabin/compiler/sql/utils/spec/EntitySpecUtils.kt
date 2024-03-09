package com.attafitamim.kabin.compiler.sql.utils.spec

import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.squareup.kotlinpoet.ClassName

fun EntitySpec.getMapperClassName(options: KabinOptions): ClassName =
    declaration.getClassName(options, KabinOptions.Key.ENTITY_MAPPER_SUFFIX)
