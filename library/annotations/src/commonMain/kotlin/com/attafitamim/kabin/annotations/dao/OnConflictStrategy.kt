package com.attafitamim.kabin.annotations.dao

enum class OnConflictStrategy {
    NONE,
    REPLACE,
    ROLLBACK,
    ABORT,
    FAIL,
    IGNORE
}
