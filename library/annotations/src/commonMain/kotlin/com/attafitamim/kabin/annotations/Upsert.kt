package com.attafitamim.kabin.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Upsert(
    val entity: KClass<*> = Any::class
)
