package com.attafitamim.kabin

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.attafitamim.kabin.core.database.configuration.KabinDatabaseConfiguration
import com.attafitamim.kabin.local.Playground
import kotlinx.coroutines.isActive

fun main() {
    val configuration = KabinDatabaseConfiguration(JdbcSqliteDriver.IN_MEMORY)
    val playground = Playground(configuration)

    playground.start()
    while (playground.scope.isActive) {
        Thread.sleep(10000)
    }
}