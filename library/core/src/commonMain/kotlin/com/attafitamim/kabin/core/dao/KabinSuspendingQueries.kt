package com.attafitamim.kabin.core.dao

import app.cash.sqldelight.SuspendingTransacterImpl
import app.cash.sqldelight.db.SqlDriver

open class KabinSuspendingQueries(driver: SqlDriver) : SuspendingTransacterImpl(driver) {

    fun createNullableArguments(count: Int?): String {
        if (count == null) return "()"
        return createArguments(count)
    }

    fun createNullableParameter(parameter: Any?): String {
        if (parameter == null) return "NULL"
        return parameter.toString()
    }

    /**
     * Used to delay enforcement of all foreign key constraints until the outermost transaction
     * is committed. Should be called only from inside a transaction.
     *
     * @see [defer_foreign_keys](https://sqlite.org/pragma.html#pragma_defer_foreign_keys)
     */
    suspend fun deferForeignKeys(enabled: Boolean = true) {
        executePragma(DEFER_FOREIGN_KEYS_PRAGMA, enabled.toPragmaValue())
    }

    /**
     * Used to enable or disable foreign keys.
     * Should be called only from outside a transaction.
     *
     * @see [foreign_keys](https://sqlite.org/pragma.html#pragma_foreign_keys)
     */
    suspend fun toggleForeignKeys(enabled: Boolean) {
        executePragma(FOREIGN_KEYS_PRAGMA, enabled.toPragmaValue())
    }

    suspend fun executePragma(name: String, value: String) {
        val sqlPragma = "PRAGMA $name = $value"
        executeSQL(sqlPragma)
    }

    suspend fun executeSQL(sql: String) {
        driver.execute(
            identifier = null,
            sql = sql,
            parameters = 0,
            binders = null
        ).await()
    }

    private companion object {
        const val DEFER_FOREIGN_KEYS_PRAGMA = "defer_foreign_keys"
        const val FOREIGN_KEYS_PRAGMA = "foreign_keys"

        const val PRAGMA_TRUE = "TRUE"
        const val PRAGMA_FALSE = "FALSE"

        fun Boolean.toPragmaValue() = when (this) {
            true -> PRAGMA_TRUE
            false -> PRAGMA_FALSE
        }
    }
 }
