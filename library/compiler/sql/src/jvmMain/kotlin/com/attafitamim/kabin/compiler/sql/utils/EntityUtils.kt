package com.attafitamim.kabin.compiler.sql.utils

import com.attafitamim.kabin.specs.entity.EntitySpec

val EntitySpec.actualTableName: String get() = tableName ?: declaration.simpleName.asString()
