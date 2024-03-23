package com.attafitamim.kabin.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Update(
    val entity: KClass<*> = Any::class,
    val onConflict: OnConflictStrategy = OnConflictStrategy.ABORT
)
