package com.attafitamim.kabin.core.driver

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.attafitamim.kabin.core.database.KabinDatabaseConfiguration
import org.w3c.dom.Worker

private const val WORKER_CODE = """new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)"""

actual fun KabinDatabaseConfiguration.createDriver(
    schema: SqlSchema<QueryResult.AsyncValue<Unit>>,
): SqlDriver = WebWorkerDriver(Worker(js(WORKER_CODE)))
