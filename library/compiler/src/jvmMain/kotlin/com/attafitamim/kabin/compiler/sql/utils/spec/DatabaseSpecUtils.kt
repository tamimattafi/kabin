package com.attafitamim.kabin.compiler.sql.utils.spec

import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.specs.database.DatabaseSpec
import com.squareup.kotlinpoet.ClassName

fun DatabaseSpec.getDatabaseClassName(options: KabinOptions): ClassName
    = declaration.getClassName(options, KabinOptions.Key.DATABASE_SUFFIX)
