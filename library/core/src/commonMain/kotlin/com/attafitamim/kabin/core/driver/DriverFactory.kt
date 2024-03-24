package com.attafitamim.kabin.core.driver

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.attafitamim.kabin.core.database.KabinDatabaseConfiguration

expect fun KabinDatabaseConfiguration.createDriver(
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
): SqlDriver
