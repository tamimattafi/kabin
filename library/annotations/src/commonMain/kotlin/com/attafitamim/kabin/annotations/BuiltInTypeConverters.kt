package com.attafitamim.kabin.annotations

@Target(allowedTargets = []) // Complex annotation target
@Retention(AnnotationRetention.BINARY)
annotation class BuiltInTypeConverters(
    val enums: State = State.INHERITED,
    val uuid: State = State.INHERITED,
) {
    enum class State {
        ENABLED,
        DISABLED,
        INHERITED
    }
}
