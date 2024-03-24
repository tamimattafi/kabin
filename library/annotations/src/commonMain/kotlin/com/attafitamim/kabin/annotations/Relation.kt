package com.attafitamim.kabin.annotations

import kotlin.reflect.KClass

@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Relation(
    val entity: KClass<*> = Any::class,
    val parentColumn: String,
    val entityColumn: String,
    val associateBy: Junction = Junction(Any::class),
    val projection: Array<String> = []
)
