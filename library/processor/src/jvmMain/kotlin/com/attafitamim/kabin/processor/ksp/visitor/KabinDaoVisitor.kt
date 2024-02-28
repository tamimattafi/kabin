package com.attafitamim.kabin.processor.ksp.visitor

import com.attafitamim.kabin.processor.handler.KabinSpecHandler
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.spec.DaoSpecProcessor
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid

class KabinDaoVisitor(
    private val specHandler: KabinSpecHandler,
    logger: KSPLogger,
    private val options: KabinOptions
) : KSVisitorVoid() {

    private val daoSpecProcessor = DaoSpecProcessor(logger)

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val daoSpec = daoSpecProcessor.getDaoSpec(classDeclaration)
        specHandler.handleDaoSpec(daoSpec)
    }
}
