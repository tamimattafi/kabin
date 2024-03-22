package com.attafitamim.kabin.core.dao

import app.cash.sqldelight.SuspendingTransacterImpl
import app.cash.sqldelight.db.SqlDriver

abstract class KabinSuspendingTransactor(driver: SqlDriver) : SuspendingTransacterImpl(driver) {

    fun createNullableArguments(count: Int?): String {
        if (count == null) return "()"
        return createArguments(count)
    }

    fun createNullableParameter(parameter: Any?): String {
        if (parameter == null) return "NULL"
        return parameter.toString()
    }
}
