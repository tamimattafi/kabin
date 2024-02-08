package com.attafitamim.kabin.annotations.index

@Target(allowedTargets = [])
@Retention(AnnotationRetention.BINARY)
annotation class Index(
    vararg val value: String,
    val orders: Array<Order> = [],
    val name: String = "",
    val unique: Boolean = false
) {
    enum class Order {
        ASC,
        DESC
    }
}
