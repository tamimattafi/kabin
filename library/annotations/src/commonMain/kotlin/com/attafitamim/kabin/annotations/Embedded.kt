package com.attafitamim.kabin.annotations

@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class Embedded(
    val prefix: String = DEFAULT_PREFIX
) {

    companion object {
        const val DEFAULT_PREFIX = ""
    }
}
