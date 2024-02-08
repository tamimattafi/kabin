package com.attafitamim.kabin.processor.utils

import com.attafitamim.kabin.specs.core.ClassSpec
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSValueArgument
import kotlin.reflect.KClass

fun KSPLogger.throwException(message: String, symbol: KSNode? = null): Nothing {
    error(message, symbol)
    val exception = Exception(message)
    exception(exception)
    throw exception
}

val KSValueArgument.stringValue: String get() = value.toString()

val KSClassDeclaration.classSpec: ClassSpec get() = ClassSpec(
    simpleName.asString(),
    packageName.asString()
)

val KClass<*>.classPackage get() = requireNotNull(qualifiedName)
    .substringBeforeLast(".$simpleName")

val KClass<*>.spec get() = ClassSpec(
    requireNotNull(simpleName),
    classPackage
)

fun Array<KClass<*>>.toClassSpecs() = map(KClass<*>::spec)
