package com.attafitamim.kabin.processor.ksp

import com.attafitamim.kabin.annotations.database.Database
import com.attafitamim.kabin.processor.handler.KabinSpecHandler
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class KabinProcessor(
    private val specHandler: KabinSpecHandler,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    private val databaseVisitor by lazy {
        KabinVisitor(specHandler, logger, options)
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(Database::class.java.name)
            .filterIsInstance<KSClassDeclaration>()

        val symbolsIterator = symbols.iterator()
        if (!symbolsIterator.hasNext()) return emptyList()

        symbolsIterator.forEach { classDeclaration ->
            runCatching {
                classDeclaration.accept(databaseVisitor, Unit)
            }.onFailure { exception ->
                logger.exception(exception)
                return@forEach
            }
        }

        return emptyList()
    }
}
