package com.attafitamim.kabin.compiler.sql.handler

import com.attafitamim.kabin.compiler.sql.utils.entity.getIndicesCreationQueries
import com.attafitamim.kabin.compiler.sql.utils.entity.tableClearQuery
import com.attafitamim.kabin.compiler.sql.utils.entity.tableCreationQuery
import com.attafitamim.kabin.compiler.sql.utils.entity.tableDropQuery
import com.attafitamim.kabin.compiler.sql.utils.poet.asListGetterPropertySpec
import com.attafitamim.kabin.compiler.sql.utils.poet.asStringGetterPropertySpec
import com.attafitamim.kabin.compiler.sql.utils.poet.writeToFile
import com.attafitamim.kabin.core.table.KabinTable
import com.attafitamim.kabin.processor.handler.KabinSpecHandler
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.dao.DaoSpec
import com.attafitamim.kabin.specs.database.DatabaseSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec

class KabinSQLSpecHandler(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: KabinOptions
) : KabinSpecHandler {

    override fun handleDatabaseSpec(databaseSpec: DatabaseSpec) {
        logger.throwException("handleDatabaseSpec: $databaseSpec", databaseSpec.declaration)
    }

    override fun handleEntitySpec(entitySpec: EntitySpec) {
        val tableFilePackage = entitySpec.declaration.packageName.asString()
        val tableFileName = buildString {
            append(entitySpec.declaration.simpleName.asString())
            append(options.getOrDefault(KabinOptions.Key.TABLE_SUFFIX))
        }

        val creationQueryProperty = KabinTable::creationQuery
            .asStringGetterPropertySpec(entitySpec.tableCreationQuery)

        val dropQueryProperty = KabinTable::dropQuery
            .asStringGetterPropertySpec(entitySpec.tableDropQuery)

        val clearQueryProperty = KabinTable::clearQuery
            .asStringGetterPropertySpec(entitySpec.tableClearQuery)

        val indicesCreationQueriesProperty = KabinTable::indicesCreationQueries
            .asListGetterPropertySpec(entitySpec.getIndicesCreationQueries(options))


        val objectClassName = ClassName(tableFilePackage, tableFileName)
        val objectSpec = TypeSpec.objectBuilder(objectClassName)
            .addSuperinterface(KabinTable::class)
            .addModifiers(KModifier.DATA)
            .addProperty(creationQueryProperty)
            .addProperty(dropQueryProperty)
            .addProperty(clearQueryProperty)
            .addProperty(indicesCreationQueriesProperty)
            .build()

        val fileSpec = FileSpec.builder(tableFilePackage, tableFileName)
            .addType(objectSpec)
            .build()

        val outputFile = codeGenerator.createNewFile(
            Dependencies(aggregating = false),
            tableFilePackage,
            tableFileName
        )

        fileSpec.writeToFile(outputFile)
    }

    override fun handleDaoSpec(daoSpec: DaoSpec) {
        logger.throwException("handleDaoSpec: $daoSpec", daoSpec.declaration)
    }
}