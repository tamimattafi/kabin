package com.attafitamim.kabin.annotations.relation

import kotlin.reflect.KClass

@Target(allowedTargets = [])
@Retention(AnnotationRetention.BINARY)
annotation class ForeignKey(
    val entity: KClass<*>,
    val parentColumns: Array<String>,
    val childColumns: Array<String>,
    val onDelete: Action = Action.NO_ACTION,
    val onUpdate: Action = Action.NO_ACTION,
    val deferred: Boolean = DEFAULT_DEFERRED
) {

    enum class Action {
        NO_ACTION,
        RESTRICT,
        SET_NULL,
        SET_DEFAULT,
        CASCADE
    }

    companion object {
        const val DEFAULT_DEFERRED = false
    }
}
