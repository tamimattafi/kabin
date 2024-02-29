package com.attafitamim.kabin.core.table

import app.cash.sqldelight.db.SqlDriver

interface KabinTable {
    fun create(driver: SqlDriver)
    fun drop(driver: SqlDriver)
    fun clear(driver: SqlDriver)
}
