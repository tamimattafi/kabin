package com.attafitamim.kabin.compiler.sql.handler

import com.attafitamim.kabin.annotations.index.Index
import com.attafitamim.kabin.compiler.sql.utils.entity.indexCreationQueries
import com.attafitamim.kabin.compiler.sql.utils.entity.tableClearQuery
import com.attafitamim.kabin.compiler.sql.utils.entity.tableCreationQuery
import com.attafitamim.kabin.compiler.sql.utils.entity.tableDropQuery
import com.attafitamim.kabin.processor.handler.KabinSpecHandler
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.dao.DaoSpec
import com.attafitamim.kabin.specs.database.DatabaseSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.attafitamim.kabin.specs.index.IndexSpec
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
            ${entitySpec.tableCreationQuery}
            
            drop:
            ${entitySpec.tableDropQuery}
            
            clear:
            ${entitySpec.tableClearQuery}
            
            indices: 
            ${entitySpec.indexCreationQueries?.joinToString("\n")}
        """.trimIndent()

        logger.throwException(scheme, entitySpec.declaration)
    }

    override fun handleDaoSpec(daoSpec: DaoSpec) {
        logger.throwException("handleDaoSpec: $daoSpec", daoSpec.declaration)
    }
}