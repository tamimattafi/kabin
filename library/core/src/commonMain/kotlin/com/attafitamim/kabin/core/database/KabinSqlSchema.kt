package com.attafitamim.kabin.core.database

import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.attafitamim.kabin.core.exceptions.SqlMigrationMissing

abstract class KabinSqlSchema(
    val migrations: List<Migration>,
    val migrationStrategy: KabinMigrationStrategy,
    override val version: Long
) : SqlSchema<QueryResult.AsyncValue<Unit>> {

    abstract suspend fun dropTables(driver: SqlDriver)
    abstract suspend fun createTables(driver: SqlDriver)

    override fun create(driver: SqlDriver): QueryResult.AsyncValue<Unit> = QueryResult.AsyncValue {
        createTables(driver)
    }

    override fun migrate(
        driver: SqlDriver,
        oldVersion: Long,
        newVersion: Long,
        vararg callbacks: AfterVersion
    ): QueryResult.AsyncValue<Unit> = QueryResult.AsyncValue {
        when {
            oldVersion == newVersion -> callbacks.notifyAll(driver)
            oldVersion > newVersion -> handleMissingMigration(oldVersion, newVersion, driver, callbacks)
            oldVersion < newVersion -> handleUpgrade(oldVersion, newVersion, driver, callbacks)
        }
    }

    private suspend fun handleUpgrade(
        oldVersion: Long,
        newVersion: Long,
        driver: SqlDriver,
        callbacks: Array<out AfterVersion>
    ) {
        val migrationCallbacksMap = callbacks.associateBy(AfterVersion::afterVersion)
        val migrationsMap = migrations.associateBy(Migration::startVersion)

        var currentVersion = oldVersion
        while (currentVersion != newVersion) {
            val migration = migrationsMap[currentVersion]
            if (migration == null) {
                handleMissingMigration(oldVersion, newVersion, driver, callbacks)
                return
            }

            migration.migrate(driver)

            for (afterVersion in currentVersion .. migration.endVersion)  {
                migrationCallbacksMap[afterVersion]?.block?.invoke(driver)
            }

            currentVersion = migration.endVersion
        }
    }

    private suspend fun handleMissingMigration(
        oldVersion: Long,
        newVersion: Long,
        driver: SqlDriver,
        callbacks: Array<out AfterVersion>
    ) = when (migrationStrategy) {
        KabinMigrationStrategy.STRICT -> migrationError(oldVersion, newVersion)
        KabinMigrationStrategy.DESTRUCTIVE -> reset(driver, callbacks)
    }

    private suspend fun reset(driver: SqlDriver, callbacks: Array<out AfterVersion>) {
        dropTables(driver)
        createTables(driver)
        callbacks.notifyAll(driver)
    }

    private fun Array<out AfterVersion>.notifyAll(driver: SqlDriver) = forEach { callback ->
        callback.block.invoke(driver)
    }

    private fun migrationError(oldVersion: Long, newVersion: Long): Nothing =
        throw SqlMigrationMissing(oldVersion, newVersion)
}