package com.attafitamim.kabin.processor.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated

class KabinProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    private val kabinVisitor by lazy {
        KabinVisitor(codeGenerator, logger, options)
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // TODO: find classes with required annotations

        return emptyList()
    }
}
