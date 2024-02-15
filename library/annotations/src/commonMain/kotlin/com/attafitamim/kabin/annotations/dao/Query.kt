package com.attafitamim.kabin.annotations.dao

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.BINARY)
annotation class Query(
    val value: String
)
