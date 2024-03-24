package com.attafitamim.kabin.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Mappers(
    vararg val value: KClass<*> = []
)
