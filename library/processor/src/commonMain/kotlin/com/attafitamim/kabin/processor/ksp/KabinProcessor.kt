package com.attafitamim.kabin.processor.ksp

import com.attafitamim.kabin.annotations.database.Database
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class KabinProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    private val kabinVisitor by lazy {
        KabinVisitor(codeGenerator, logger, options)
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver
            .getSymbolsWithAnnotation(Database::class.java.name)
            .filterIsInstance<KSClassDeclaration>()

        val symbolsIterator = symbols.iterator()
        if (!symbolsIterator.hasNext()) return emptyList()

        val unprocessedSymbols = ArrayList<KSAnnotated>()
        symbolsIterator.forEach { classDeclaration ->
            kotlin.runCatching {
                classDeclaration.accept(kabinVisitor, Unit)
            }.onFailure {
                unprocessedSymbols.add(classDeclaration)
            }
        }

        return unprocessedSymbols
    }
}
