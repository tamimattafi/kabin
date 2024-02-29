package com.attafitamim.kabin.processor.handler

import com.attafitamim.kabin.specs.database.DatabaseSpec

interface KabinSpecHandler {
    fun handleDatabaseSpec(databaseSpec: DatabaseSpec)
}
