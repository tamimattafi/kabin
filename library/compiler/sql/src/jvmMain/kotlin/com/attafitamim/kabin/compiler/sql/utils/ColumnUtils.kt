package com.attafitamim.kabin.compiler.sql.utils

import com.attafitamim.kabin.specs.column.ColumnSpec

val ColumnSpec.actualName: String get() = name ?: declaration.simpleName.asString()
