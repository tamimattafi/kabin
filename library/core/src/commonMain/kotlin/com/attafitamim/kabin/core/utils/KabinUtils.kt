package com.attafitamim.kabin.core.utils

import app.cash.sqldelight.SuspendingTransactionWithReturn
import app.cash.sqldelight.SuspendingTransactionWithoutReturn
import app.cash.sqldelight.TransactionCallbacks
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.attafitamim.kabin.core.dao.KabinSuspendingQueries
import com.attafitamim.kabin.core.database.configuration.KabinDatabaseConfiguration

suspend inline fun KabinSuspendingQueries.safeTransaction(
    configuration: KabinDatabaseConfiguration,
    crossinline body: suspend SuspendingTransactionWithoutReturn.() -> Unit
) = transaction {
    tryDifferForeignKeys(configuration)
    body()
}

suspend inline fun <T> KabinSuspendingQueries.safeTransactionWithResult(
    configuration: KabinDatabaseConfiguration,
    crossinline body: suspend SuspendingTransactionWithReturn<T>.() -> T
): T = transactionWithResult {
    tryDifferForeignKeys(configuration)
    body()
}

inline fun SqlDriver.safeGlobalTransaction(
    configuration: KabinDatabaseConfiguration,
    noinline body: suspend TransactionCallbacks.() -> Unit
): QueryResult.AsyncValue<Unit> = KabinSuspendingQueries(this)
    .safeGlobalTransaction(configuration, body)

inline fun KabinSuspendingQueries.safeGlobalTransaction(
    configuration: KabinDatabaseConfiguration,
    noinline body: suspend TransactionCallbacks.() -> Unit
) = QueryResult.AsyncValue {
    try {
        tryToggleForeignKeys(configuration, enabled = false)
        safeTransaction(configuration, body = body)
    } finally {
        tryToggleForeignKeys(configuration, enabled = true)
    }
}

suspend fun KabinSuspendingQueries.tryToggleForeignKeys(
    configuration: KabinDatabaseConfiguration,
    enabled: Boolean
) {
    if (configuration.extendedConfig.foreignKeyConstraintsEnabled) {
        toggleForeignKeys(enabled)
    }
}

suspend fun KabinSuspendingQueries.tryDifferForeignKeys(
    configuration: KabinDatabaseConfiguration
) = with(configuration.extendedConfig) {
    if (foreignKeyConstraintsEnabled && deferForeignKeysInsideTransaction) {
        deferForeignKeys()
    }
}