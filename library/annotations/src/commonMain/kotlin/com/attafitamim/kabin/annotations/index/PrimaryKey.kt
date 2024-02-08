package com.attafitamim.kabin.annotations.index

@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class PrimaryKey(
    val autoGenerate: Boolean = false
)
