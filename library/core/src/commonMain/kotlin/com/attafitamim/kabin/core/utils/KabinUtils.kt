package com.attafitamim.kabin.core.utils

import app.cash.sqldelight.SuspendingTransactionWithReturn
import app.cash.sqldelight.SuspendingTransactionWithoutReturn
import app.cash.sqldelight.TransactionCallbacks
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.attafitamim.kabin.core.dao.KabinSuspendingQueries
import com.attafitamim.kabin.core.database.configuration.KabinDatabaseConfiguration

private const val DEFER_FOREIGN_KEYS_PRAGMA = "defer_foreign_keys"
private const val FOREIGN_KEYS_PRAGMA = "foreign_keys"

private const val PRAGMA_ENABLED = "ON"
private const val PRAGMA_DISABLED = "OFF"

fun Boolean.toPragmaValue() = when (this) {
    true -> PRAGMA_ENABLED
    false -> PRAGMA_DISABLED
}

suspend inline fun KabinSuspendingQueries.safeTransaction(
    configuration: KabinDatabaseConfiguration,
    crossinline body: suspend SuspendingTransactionWithoutReturn.() -> Unit
) = transaction {
    tryDifferForeignKeys(configuration, enabled = true)
    body()
}

suspend inline fun <T> KabinSuspendingQueries.safeTransactionWithResult(
    configuration: KabinDatabaseConfiguration,
    crossinline body: suspend SuspendingTransactionWithReturn<T>.() -> T
): T = transactionWithResult {
    tryDifferForeignKeys(configuration, enabled = true)
    body()
}

inline fun SqlDriver.safeGlobalQuery(
    configuration: KabinDatabaseConfiguration,
    noinline body: suspend () -> Unit
): QueryResult.AsyncValue<Unit> = QueryResult.AsyncValue {
    try {
        tryToggleForeignKeys(configuration, enabled = false)
        tryDifferForeignKeys(configuration, enabled = true)
        body()
    } finally {
        tryToggleForeignKeys(configuration, enabled = true)
    }
}

suspend inline fun KabinSuspendingQueries.safeGlobalTransaction(
    configuration: KabinDatabaseConfiguration,
    noinline body: suspend TransactionCallbacks.() -> Unit
) = try {
    tryToggleForeignKeys(configuration, enabled = false)
    safeTransaction(configuration, body = body)
} finally {
    tryToggleForeignKeys(configuration, enabled = true)
}

/**
 * Used to delay enforcement of all foreign key constraints until the outermost transaction
 * is committed. Should be called only from inside a transaction.
 *
 * @see [defer_foreign_keys](https://sqlite.org/pragma.html#pragma_defer_foreign_keys)
 */
fun SqlDriver.deferForeignKeys(enabled: Boolean) {
    executePragma(DEFER_FOREIGN_KEYS_PRAGMA, enabled.toPragmaValue())
}

/**
 * Used to enable or disable foreign keys.
 * Should be called only from outside a transaction.
 *
 * @see [foreign_keys](https://sqlite.org/pragma.html#pragma_foreign_keys)
 */
fun SqlDriver.toggleForeignKeys(enabled: Boolean) {
    executePragma(FOREIGN_KEYS_PRAGMA, enabled.toPragmaValue())
}

fun SqlDriver.tryToggleForeignKeys(
    configuration: KabinDatabaseConfiguration,
    enabled: Boolean
) {
    if (configuration.extendedConfig.foreignKeyConstraintsEnabled) {
        toggleForeignKeys(enabled)
    }
}

fun SqlDriver.tryDifferForeignKeys(
    configuration: KabinDatabaseConfiguration,
    enabled: Boolean
) = with(configuration.extendedConfig) {
    if (foreignKeyConstraintsEnabled && deferForeignKeysInsideTransaction) {
        deferForeignKeys(enabled)
    }
}

fun SqlDriver.executePragma(name: String, value: String) {
    val sqlPragma = "PRAGMA $name = $value;"
    executeSQL(sqlPragma)
}

fun SqlDriver.executeSQL(sql: String) {
    execute(
        identifier = null,
        sql = sql,
        parameters = 0,
        binders = null
    )
}