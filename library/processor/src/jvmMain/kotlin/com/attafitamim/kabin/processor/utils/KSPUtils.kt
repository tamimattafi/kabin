package com.attafitamim.kabin.processor.utils

import com.attafitamim.kabin.processor.exceptions.KabinProcessorException
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSValueArgument
import kotlin.reflect.KClass

fun KSPLogger.throwException(message: String, symbol: KSNode? = null): Nothing {
    error(message, symbol)
    throw KabinProcessorException(message)
}

val KSValueArgument.stringValue: String get() = value.toString()

val KSAnnotation.argumentsMap get() = arguments.associateBy {
        argument -> requireNotNull(argument.name).asString()
}

fun <T : Any> KSAnnotation.isSame(annotationClass: KClass<T>): Boolean =
    shortName.getShortName() == annotationClass.simpleName &&
            annotationType.resolve().declaration.qualifiedName?.asString() ==
            annotationClass.qualifiedName

inline fun <reified T : Any> Map<String, KSValueArgument>.getArgument(name: String): T? =
    getValue(name).value as? T

inline fun <reified T : Any> Map<String, KSValueArgument>.requireArgument(name: String): T =
    requireNotNull(getArgument(name))
