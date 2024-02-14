package com.attafitamim.kabin.processor.ksp

import com.attafitamim.kabin.processor.handler.KabinSpecHandler
import com.attafitamim.kabin.specs.database.DatabaseSpec
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class KabinProcessorProvider : SymbolProcessorProvider {

    private val specHandler = object : KabinSpecHandler {
        override fun handleDatabaseSpec(databaseSpec: DatabaseSpec) {

        }
    }

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        KabinProcessor(
            specHandler,
            environment.logger,
            environment.options
        )
}
