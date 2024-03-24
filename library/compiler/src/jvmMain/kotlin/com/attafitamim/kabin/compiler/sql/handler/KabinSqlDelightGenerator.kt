package com.attafitamim.kabin.compiler.sql.handler

import com.attafitamim.kabin.compiler.sql.generator.database.DatabaseGenerator
import com.attafitamim.kabin.processor.handler.KabinSpecHandler
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.database.DatabaseSpec
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import kotlin.math.log

class KabinSqlDelightGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: KabinOptions
) : KabinSpecHandler {

    private val databaseGenerator = DatabaseGenerator(codeGenerator, logger, options)

    override fun handleDatabaseSpec(databaseSpec: DatabaseSpec) {
        databaseGenerator.generate(databaseSpec)
    }
}
