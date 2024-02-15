package com.attafitamim.kabin.annotations.dao

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Delete(
    val entity: KClass<*> = Any::class
)
