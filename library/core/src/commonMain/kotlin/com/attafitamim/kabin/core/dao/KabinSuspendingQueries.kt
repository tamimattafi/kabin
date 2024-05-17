package com.attafitamim.kabin.core.dao

import app.cash.sqldelight.SuspendingTransacterImpl
import app.cash.sqldelight.db.SqlDriver
import com.attafitamim.kabin.core.database.configuration.KabinDatabaseConfiguration
import com.attafitamim.kabin.core.utils.tryDifferForeignKeys
import com.attafitamim.kabin.core.utils.tryToggleForeignKeys

open class KabinSuspendingQueries(driver: SqlDriver) : SuspendingTransacterImpl(driver) {

    fun createNullableArguments(count: Int?): String {
        if (count == null) return "()"
        return createArguments(count)
    }

    fun createNullableParameter(parameter: Any?): String {
        if (parameter == null) return "NULL"
        return parameter.toString()
    }

    suspend fun tryToggleForeignKeys(
        configuration: KabinDatabaseConfiguration,
        enabled: Boolean
    ) = driver.tryToggleForeignKeys(configuration, enabled)

    suspend fun tryDifferForeignKeys(
        configuration: KabinDatabaseConfiguration,
        enabled: Boolean
    ) = driver.tryDifferForeignKeys(configuration, enabled)
}
