package com.attafitamim.kabin.compiler.sql.utils.spec

import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName

fun KSClassDeclaration.getClassName(
    options: KabinOptions,
    suffixKey: KabinOptions.Key
): ClassName {
    val classPackage = packageName.asString()
    val suffix = options.getOrDefault(suffixKey)
    val className = buildString {
        append(simpleName.asString())
        append(suffix)
    }

    return ClassName(classPackage, className)
}
