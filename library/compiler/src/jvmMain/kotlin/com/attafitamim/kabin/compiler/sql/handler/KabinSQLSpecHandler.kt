package com.attafitamim.kabin.compiler.sql.handler

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.SuspendingTransacterImpl
import app.cash.sqldelight.db.SqlDriver
import com.attafitamim.kabin.compiler.sql.utils.poet.adapter.ColumnAdapterReference
import com.attafitamim.kabin.compiler.sql.utils.poet.adapter.getPropertyName
import com.attafitamim.kabin.compiler.sql.utils.poet.addParameter
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.getIndicesCreationQueries
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.tableClearQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.tableCreationQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.tableDropQuery
import com.attafitamim.kabin.compiler.sql.utils.poet.buildSpec
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.addQueryFunction
import com.attafitamim.kabin.compiler.sql.utils.poet.entity.addEntityParseFunction
import com.attafitamim.kabin.compiler.sql.utils.poet.entity.supportedAffinity
import com.attafitamim.kabin.compiler.sql.utils.poet.parameterName
import com.attafitamim.kabin.compiler.sql.utils.poet.sqldelight.addDriverExecutionCode
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
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
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

        val entityClassName = entitySpec.declaration.toClassName()

        val createFunctionSpec = KabinTable::create.buildSpec()
            .addModifiers(KModifier.OVERRIDE)
            .addDriverExecutionCode(entitySpec.tableCreationQuery)
            .apply {
                entitySpec.getIndicesCreationQueries(options)?.forEach { index ->
                    addDriverExecutionCode(index)
                }
            }.build()

        val dropFunctionSpec = KabinTable::drop.buildSpec()
            .addModifiers(KModifier.OVERRIDE)
            .addDriverExecutionCode(entitySpec.tableDropQuery)
            .build()

        val clearFunctionSpec = KabinTable::clear.buildSpec()
            .addModifiers(KModifier.OVERRIDE)
            .addDriverExecutionCode(entitySpec.tableClearQuery)
            .build()

        val mapClassName = KabinTable.EntityMapper::class.asClassName()
        val mapSuperClassName = mapClassName.parameterizedBy(entityClassName)

        val mapClassBuilder = TypeSpec.classBuilder(mapClassName)
            .addSuperinterface(mapSuperClassName)

        val adapters = mapClassBuilder
            .addEntityParseFunction(entitySpec)

        val constructorBuilder = FunSpec.constructorBuilder()

        adapters.forEach { adapter ->
            val propertyName = adapter.getPropertyName()
            val affinityType = supportedAffinity.getValue(adapter.affinityType)
            val adapterType = ColumnAdapter::class.asClassName()
                .parameterizedBy(adapter.kotlinType, affinityType.asClassName())

            val propertySpec = PropertySpec.builder(
                propertyName,
                adapterType,
                KModifier.PRIVATE
            ).initializer(propertyName).build()

            mapClassBuilder.addProperty(propertySpec)

            constructorBuilder.addParameter(
                adapter.getPropertyName(),
                adapterType
            )
        }

        mapClassBuilder.primaryConstructor(constructorBuilder.build())

        val className = ClassName(tableFilePackage, tableFileName)
        val superClassName = KabinTable::class.asClassName()

        val classBuilder = TypeSpec.classBuilder(className)
            .addSuperinterface(superClassName)
            .addFunction(createFunctionSpec)
            .addFunction(dropFunctionSpec)
            .addFunction(clearFunctionSpec)
            .addType(mapClassBuilder.build())

        val fileSpec = FileSpec.builder(tableFilePackage, tableFileName)
            .addType(classBuilder.build())
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

        val className = ClassName(daoFilePackage, daoFileName)
        val superClassName = SuspendingTransacterImpl::class.asClassName()

        val classBuilder = TypeSpec.classBuilder(className)
            .superclass(superClassName)
            .addSuperclassConstructorParameter(parameterName<SqlDriver>())

        val adapters = HashSet<ColumnAdapterReference>()
        daoSpec.functionSpecs.forEach { functionSpec ->
            if (functionSpec.actionSpec != null) {
                val functionAdapters = classBuilder.addQueryFunction(functionSpec)
                adapters.addAll(functionAdapters)
            }
        }

        val constructorBuilder = FunSpec.constructorBuilder()
            .addParameter<SqlDriver>()

        adapters.forEach { adapter ->
            val propertyName = adapter.getPropertyName()
            val affinityType = supportedAffinity.getValue(adapter.affinityType)
            val adapterType = ColumnAdapter::class.asClassName()
                .parameterizedBy(adapter.kotlinType, affinityType.asClassName())

            val propertySpec = PropertySpec.builder(
                propertyName,
                adapterType,
                KModifier.PRIVATE
            ).initializer(propertyName).build()

            classBuilder.addProperty(propertySpec)

            constructorBuilder.addParameter(
                adapter.getPropertyName(),
                adapterType
            )
        }

        classBuilder.primaryConstructor(constructorBuilder.build())

        val classSpec = classBuilder.build()
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