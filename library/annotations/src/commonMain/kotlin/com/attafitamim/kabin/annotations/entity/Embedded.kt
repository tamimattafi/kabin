package com.attafitamim.kabin.annotations.entity

@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Embedded(
    val prefix: String = DEFAULT_PREFIX
) {

    companion object {
        const val DEFAULT_PREFIX = ""
    }
}
