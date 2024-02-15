package com.attafitamim.kabin.processor.ksp

import com.attafitamim.kabin.processor.handler.KabinSpecHandler
import com.attafitamim.kabin.specs.database.DatabaseSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class KabinProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor =
        KabinProcessor(
            createSpecHandler(environment),
            environment.logger,
            environment.options
        )

    private fun createSpecHandler(environment: SymbolProcessorEnvironment) =
        object : KabinSpecHandler {
            override fun handleDatabaseSpec(databaseSpec: DatabaseSpec) {
                environment.logger.error(
                    "DatabaseSpec: $databaseSpec",
                    databaseSpec.declaration
                )
            }

            override fun handleEntitySpec(entitySpec: EntitySpec) {
                environment.logger.error(
                    "EntitySpec: $entitySpec",
                    entitySpec.declaration
                )
            }
        }
}
