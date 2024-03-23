package com.attafitamim.kabin.annotations

@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class PrimaryKey(
    val autoGenerate: Boolean = DEFAULT_AUTO_GENERATE
) {

    companion object {
        const val DEFAULT_AUTO_GENERATE = false
    }
}
