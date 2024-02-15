package com.attafitamim.kabin.annotations.dao

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class RawQuery(
    val observedEntities: Array<KClass<*>> = []
)
