package com.attafitamim.kabin.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class AutoMigration(
    val from: Int,
    val to: Int,
    val spec: KClass<*> = Any::class
)
