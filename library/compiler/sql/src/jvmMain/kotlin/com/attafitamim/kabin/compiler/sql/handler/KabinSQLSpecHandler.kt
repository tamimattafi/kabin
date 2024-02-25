package com.attafitamim.kabin.compiler.sql.handler

import com.attafitamim.kabin.compiler.sql.utils.sqlClearQuery
import com.attafitamim.kabin.compiler.sql.utils.sqlCreationQuery
import com.attafitamim.kabin.compiler.sql.utils.sqlDropQuery
import com.attafitamim.kabin.processor.handler.KabinSpecHandler
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.dao.DaoSpec
import com.attafitamim.kabin.specs.database.DatabaseSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger

class KabinSQLSpecHandler(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: KabinOptions
) : KabinSpecHandler {

    override fun handleDatabaseSpec(databaseSpec: DatabaseSpec) {
        logger.throwException("handleDatabaseSpec: $databaseSpec", databaseSpec.declaration)
    }

    override fun handleEntitySpec(entitySpec: EntitySpec) {
        val scheme = """
            
            create:
            ${entitySpec.sqlCreationQuery}
            
            drop:
            ${entitySpec.sqlDropQuery}
            
            clear:
            ${entitySpec.sqlClearQuery}
        """.trimIndent()

        logger.throwException(scheme, entitySpec.declaration)
    }

    override fun handleDaoSpec(daoSpec: DaoSpec) {
        logger.throwException("handleDaoSpec: $daoSpec", daoSpec.declaration)
    }
}