package com.attafitamim.kabin.compiler.sql.handler

import app.cash.sqldelight.Query
import app.cash.sqldelight.SuspendingTransacterImpl
import app.cash.sqldelight.db.SqlDriver
import com.attafitamim.kabin.compiler.sql.utils.poet.DRIVER_NAME
import com.attafitamim.kabin.compiler.sql.utils.poet.addParameter
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.getIndicesCreationQueries
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.tableClearQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.tableCreationQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.tableDropQuery
import com.attafitamim.kabin.compiler.sql.utils.poet.asListGetterPropertySpec
import com.attafitamim.kabin.compiler.sql.utils.poet.asStringGetterPropertySpec
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.buildQueriesSpec
import com.attafitamim.kabin.compiler.sql.utils.poet.parameterName
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
import com.google.devtools.ksp.symbol.ClassKind
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName

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
        generateDaoQueries(daoSpec)
    }

    private fun generateDao(daoSpec: DaoSpec) {
        val daoFilePackage = daoSpec.declaration.packageName.asString()
        val daoFileName = buildString {
            append(daoSpec.declaration.simpleName.asString())
            append(options.getOrDefault(KabinOptions.Key.DAO_SUFFIX))
        }

        val className = ClassName(daoFilePackage, daoFileName)
        val superClassName = daoSpec.declaration.toClassName()
        val classSpecBuilder = TypeSpec.classBuilder(className)

        if (daoSpec.declaration.classKind == ClassKind.INTERFACE) {
            classSpecBuilder.addSuperinterface(superClassName)
        } else {
            classSpecBuilder.superclass(superClassName)
        }

        daoSpec.functionSpecs.forEach { functionSpec ->
            if (functionSpec.actionSpec != null) {
             /*   val spec = functionSpec.buildSpec()
                    .addModifiers(KModifier.OVERRIDE)
                    .build()

                classSpecBuilder.addFunction(spec)*/
            }
        }

        val classSpec = classSpecBuilder.build()
        val fileSpec = FileSpec.builder(daoFilePackage, daoFileName)
            .addType(classSpec)
            .build()

        val outputFile = codeGenerator.createNewFile(
            Dependencies(aggregating = false),
            daoFilePackage,
            daoFileName
        )

        fileSpec.writeToFile(outputFile)
    }

    private fun generateDaoQueries(daoSpec: DaoSpec) {
        val daoFilePackage = daoSpec.declaration.packageName.asString()
        val daoFileName = buildString {
            append(daoSpec.declaration.simpleName.asString())
            append(options.getOrDefault(KabinOptions.Key.DAO_QUERIES_SUFFIX))
        }


        val constructor = FunSpec.constructorBuilder()
            .addParameter<SqlDriver>()
            .build()

        val className = ClassName(daoFilePackage, daoFileName)
        val superClassName = SuspendingTransacterImpl::class.asClassName()
        val classSpecBuilder = TypeSpec.classBuilder(className)
            .superclass(superClassName)
            .primaryConstructor(constructor)
            .addSuperclassConstructorParameter(parameterName<SqlDriver>())

        daoSpec.functionSpecs.forEach { functionSpec ->
            if (functionSpec.actionSpec != null) {
                val spec = functionSpec.buildQueriesSpec()
                classSpecBuilder.addFunction(spec)
            }
        }

        val classSpec = classSpecBuilder.build()
        val fileSpec = FileSpec.builder(daoFilePackage, daoFileName)
            .addType(classSpec)
            .build()

        val outputFile = codeGenerator.createNewFile(
            Dependencies(aggregating = false),
            daoFilePackage,
            daoFileName
        )

        fileSpec.writeToFile(outputFile)
    }
}