package com.attafitamim.kabin.processor.ksp.visitor

import com.attafitamim.kabin.processor.handler.KabinSpecHandler
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.spec.DatabaseSpecProcessor
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid

class KabinDatabaseVisitor(
    private val specHandler: KabinSpecHandler,
    logger: KSPLogger,
    private val options: KabinOptions
) : KSVisitorVoid() {

    private val databaseSpecProcessor = DatabaseSpecProcessor(logger)

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val databaseSpec = databaseSpecProcessor.getDatabaseSpec(classDeclaration)
        specHandler.handleDatabaseSpec(databaseSpec)
    }
}
