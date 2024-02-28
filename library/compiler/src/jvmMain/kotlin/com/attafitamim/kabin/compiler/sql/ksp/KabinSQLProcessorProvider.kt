package com.attafitamim.kabin.compiler.sql.ksp

import com.attafitamim.kabin.compiler.sql.handler.KabinSQLSpecHandler
import com.attafitamim.kabin.processor.ksp.KabinProcessor
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class KabinSQLProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val options = KabinOptions(environment.options)

        val handler = KabinSQLSpecHandler(
            environment.codeGenerator,
            environment.logger,
            options
        )

        return KabinProcessor(
            handler,
            environment.logger,
            options
        )
    }
}
