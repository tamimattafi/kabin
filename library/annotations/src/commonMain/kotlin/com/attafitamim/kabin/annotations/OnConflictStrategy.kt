package com.attafitamim.kabin.annotations

enum class OnConflictStrategy {
    NONE,
    REPLACE,
    ROLLBACK,
    ABORT,
    FAIL,
    IGNORE
}
