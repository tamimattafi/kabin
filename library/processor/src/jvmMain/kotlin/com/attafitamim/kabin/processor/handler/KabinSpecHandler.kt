package com.attafitamim.kabin.processor.handler

import com.attafitamim.kabin.specs.database.DatabaseSpec
import com.attafitamim.kabin.specs.entity.EntitySpec

interface KabinSpecHandler {
    fun handleDatabaseSpec(databaseSpec: DatabaseSpec)
    fun handleEntitySpec(entitySpec: EntitySpec)
}
