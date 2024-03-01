package com.attafitamim.kabin.annotations.converters

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class TypeConverters(
    vararg val value: KClass<*> = [],
    val builtInTypeConverters: BuiltInTypeConverters = BuiltInTypeConverters()
)