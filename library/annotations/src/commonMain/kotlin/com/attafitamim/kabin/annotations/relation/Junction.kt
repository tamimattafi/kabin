package com.attafitamim.kabin.annotations.relation

import kotlin.reflect.KClass

@Target(allowedTargets = []) // Complex annotation target
@Retention(AnnotationRetention.BINARY)
annotation class Junction(
    val value: KClass<*>,
    val parentColumn: String = "",
    val entityColumn: String = ""
)
