package com.attafitamim.kabin.annotations.converters

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Mappers(
    vararg val value: KClass<*> = []
)
