package com.attafitamim.kabin.processor.utils

import com.attafitamim.kabin.processor.exceptions.KabinProcessorException
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotated
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

fun KSAnnotated.requireAnnotationArgumentsMap(
    annotationClass: KClass<*>
) = requireNotNull(getAnnotationArgumentsMap(annotationClass))

fun KSAnnotated.getAnnotationArgumentsMap(
    annotationClass: KClass<*>
) = annotations.firstOrNull { annotation ->
    annotation.isSame(annotationClass)
}?.argumentsMap

fun <T : Any> KSAnnotation.isSame(annotationClass: KClass<T>): Boolean =
    shortName.getShortName() == annotationClass.simpleName &&
            annotationType.resolve().declaration.qualifiedName?.asString() ==
            annotationClass.qualifiedName

inline fun <reified T : Any> Map<String, KSValueArgument>.getArgument(name: String): T? =
    get(name)?.value as? T

inline fun <reified T : Any> Map<String, KSValueArgument>.getArgument(
    name: String,
    default: T
): T = getArgument(name) ?: default

inline fun <reified T : Any> Map<String, KSValueArgument>.requireArgument(name: String): T =
    requireNotNull(getArgument(name))
