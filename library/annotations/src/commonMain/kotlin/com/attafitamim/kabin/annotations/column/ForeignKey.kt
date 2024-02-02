package com.attafitamim.kabin.annotations.column

import kotlin.reflect.KClass

@Target(allowedTargets = [])
@Retention(AnnotationRetention.BINARY)
annotation class ForeignKey(
    val entity: KClass<*>,
    val parentColumns: Array<String>,
    val childColumns: Array<String>,
    val onDelete: Action = Action.NO_ACTION,
    val onUpdate: Action = Action.NO_ACTION,
    val deferred: Boolean = false
) {

    enum class Action {
        NO_ACTION,
        RESTRICT,
        SET_NULL,
        SET_DEFAULT,
        CASCADE
    }
}
