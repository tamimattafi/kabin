package com.attafitamim.kabin.core.database

import app.cash.sqldelight.db.SqlDriver
import com.attafitamim.kabin.core.dao.KabinSuspendingQueries
import com.attafitamim.kabin.core.database.configuration.KabinDatabaseConfiguration
import com.attafitamim.kabin.core.utils.safeGlobalTransaction

abstract class KabinBaseDatabase(
    val driver: SqlDriver,
    val configuration: KabinDatabaseConfiguration
) : KabinDatabase {

    private val queries = KabinSuspendingQueries(driver)

    abstract suspend fun clearTables()

    final override suspend fun clear() {
        queries.safeGlobalTransaction(configuration) {
            clearTables()
        }
    }
}