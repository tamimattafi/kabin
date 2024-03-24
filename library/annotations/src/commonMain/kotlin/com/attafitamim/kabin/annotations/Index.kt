package com.attafitamim.kabin.annotations

@Target(allowedTargets = [])
@Retention(AnnotationRetention.BINARY)
annotation class Index(
    vararg val columns: String,
    val orders: Array<Order> = [],
    val name: String = DEFAULT_NAME,
    val unique: Boolean = DEFAULT_UNIQUE
) {
    enum class Order {
        ASC,
        DESC
    }

    companion object {
        const val DEFAULT_NAME = ""
        const val DEFAULT_UNIQUE = false
    }
}
