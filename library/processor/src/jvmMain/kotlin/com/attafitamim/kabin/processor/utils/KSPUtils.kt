package com.attafitamim.kabin.processor.utils

import com.attafitamim.kabin.processor.exceptions.KabinProcessorException
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSNode
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueArgument
import kotlin.reflect.KClass

const val SYMBOL_SEPARATOR = "."

val KSValueArgument.stringValue: String get() = value.toString()

val KSType.classDeclaration get() = declaration as KSClassDeclaration

val KSAnnotation.argumentsMap get() = arguments.associateBy {
    argument -> requireNotNull(argument.name).asString()
}

fun KSPLogger.throwException(message: String, symbol: KSNode? = null): Nothing {
    error(message, symbol)
    throw KabinProcessorException(message)
}

fun KSAnnotated.requireAnnotationArgumentsMap(
    annotationClass: KClass<*>
) = requireNotNull(getAnnotationArgumentsMap(annotationClass))

fun KSAnnotated.getAnnotationArgumentsMap(
    annotationClass: KClass<*>
) = annotations.firstOrNull { annotation ->
    annotation.isInstanceOf(annotationClass)
}?.argumentsMap

inline fun <reified T : Enum<T>> KSType.asEnum(): T {
    val enumName = this.toString().substringAfterLast(SYMBOL_SEPARATOR)
    return enumValueOf(enumName)
}

fun <T : Any> KSAnnotation.isInstanceOf(annotationClass: KClass<T>): Boolean =
    shortName.getShortName() == annotationClass.simpleName &&
            annotationType.resolve().declaration.qualifiedName?.asString() ==
            annotationClass.qualifiedName

inline fun <reified T : Any> Map<String, KSValueArgument>.getArgument(name: String): T? =
    get(name)?.value as? T

fun Map<String, KSValueArgument>.getClassDeclaration(name: String): KSClassDeclaration? =
    getArgument<KSType>(name)?.declaration as? KSClassDeclaration

fun Map<String, KSValueArgument>.getClassDeclarations(name: String): List<KSClassDeclaration>? =
    getArgument<List<KSType>>(name)?.map { type ->
        type.declaration as KSClassDeclaration
    }

fun Map<String, KSValueArgument>.requireClassDeclarations(name: String): List<KSClassDeclaration> =
    requireNotNull(getClassDeclarations(name))

fun Map<String, KSValueArgument>.requireClassDeclaration(name: String): KSClassDeclaration =
    requireNotNull(getClassDeclaration(name))

fun KSTypeReference.resolveClassDeclaration() = resolve().classDeclaration

inline fun <reified T : Enum<T>> Map<String, KSValueArgument>.getEnumArgument(name: String): T? =
    getArgument<KSType>(name)?.asEnum<T>()

inline fun <reified T : Enum<T>> Map<String, KSValueArgument>.getEnumsArgument(name: String): List<T>? =
    getArgument<List<KSType>>(name)?.map(KSType::asEnum)

inline fun <reified T : Any> Map<String, KSValueArgument>.getArgument(
    name: String,
    default: T
): T = getArgument(name) ?: default

inline fun <reified T : Any> Map<String, KSValueArgument>.requireArgument(name: String): T =
    requireNotNull(getArgument(name))
