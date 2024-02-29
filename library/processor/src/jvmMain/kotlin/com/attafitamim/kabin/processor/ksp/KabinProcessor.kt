package com.attafitamim.kabin.processor.ksp

import com.attafitamim.kabin.annotations.database.Database
import com.attafitamim.kabin.processor.handler.KabinSpecHandler
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.ksp.visitor.KabinDatabaseVisitor
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import kotlin.reflect.KClass

class KabinProcessor(
    private val specHandler: KabinSpecHandler,
    private val logger: KSPLogger,
    private val options: KabinOptions
) : SymbolProcessor {

    private val databaseVisitor by lazy {
        KabinDatabaseVisitor(specHandler, logger, options)
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        databaseVisitor.visitAnnotatedClass(resolver, Database::class)
        return emptyList()
    }

    private fun KSVisitorVoid.visitAnnotatedClass(
        resolver: Resolver,
        annotationClass: KClass<*>
    ) {
        val symbols = resolver
            .getSymbolsWithAnnotation(annotationClass.java.name)
            .filterIsInstance<KSClassDeclaration>()

        visit(symbols)
    }

    private fun KSVisitorVoid.visit(symbols: Sequence<KSClassDeclaration>) {
        val symbolsIterator = symbols.iterator()
        if (symbolsIterator.hasNext()) {
            symbolsIterator.forEach { classDeclaration ->
                runCatching {
                    classDeclaration.accept(this, Unit)
                }.onFailure { exception ->
                    logger.exception(exception)
                }
            }
        }
    }
}
