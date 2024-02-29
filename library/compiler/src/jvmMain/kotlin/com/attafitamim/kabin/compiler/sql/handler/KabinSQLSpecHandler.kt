package com.attafitamim.kabin.compiler.sql.handler

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.Query
import app.cash.sqldelight.SuspendingTransacter
import app.cash.sqldelight.SuspendingTransacterImpl
import app.cash.sqldelight.db.SqlDriver
import com.attafitamim.kabin.compiler.sql.utils.poet.DRIVER_NAME
import com.attafitamim.kabin.compiler.sql.utils.poet.references.ColumnAdapterReference
import com.attafitamim.kabin.compiler.sql.utils.poet.references.getPropertyName
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.getIndicesCreationQueries
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.tableClearQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.tableCreationQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.tableDropQuery
import com.attafitamim.kabin.compiler.sql.utils.poet.buildSpec
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.addQueryFunction
import com.attafitamim.kabin.compiler.sql.utils.poet.entity.addEntityParseFunction
import com.attafitamim.kabin.compiler.sql.utils.poet.entity.supportedAffinity
import com.attafitamim.kabin.compiler.sql.utils.poet.references.MapperReference
import com.attafitamim.kabin.compiler.sql.utils.poet.sqldelight.addDriverExecutionCode
import com.attafitamim.kabin.compiler.sql.utils.poet.toLowerCamelCase
import com.attafitamim.kabin.compiler.sql.utils.poet.writeToFile
import com.attafitamim.kabin.core.dao.KabinDao
import com.attafitamim.kabin.core.table.KabinTable
import com.attafitamim.kabin.processor.handler.KabinSpecHandler
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.core.TypeDeclaration
import com.attafitamim.kabin.specs.dao.DaoActionSpec
import com.attafitamim.kabin.specs.dao.DaoSpec
import com.attafitamim.kabin.specs.database.DatabaseSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
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

        val mapClassName = KabinTable.Mapper::class.asClassName()
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
        generateDao(daoSpec)
    }

    private fun generateDao(daoSpec: DaoSpec) {
        val daoFilePackage = daoSpec.declaration.packageName.asString()
        val daoFileName = buildString {
            append(daoSpec.declaration.simpleName.asString())
            append(options.getOrDefault(KabinOptions.Key.DAO_SUFFIX))
        }

        val daoQueriesFilePackage = daoSpec.declaration.packageName.asString()
        val daoQueriesFileName = buildString {
            append(daoSpec.declaration.simpleName.asString())
            append(options.getOrDefault(KabinOptions.Key.DAO_QUERIES_SUFFIX))
        }

        val className = ClassName(daoFilePackage, daoFileName)
        val daoQueriesClassName = ClassName(daoQueriesFilePackage, daoQueriesFileName)

        val superClassName = daoSpec.declaration.toClassName()
        val kabinDaoInterface = KabinDao::class.asClassName()
            .parameterizedBy(daoQueriesClassName)

        val classBuilder = TypeSpec.classBuilder(className)
            .addSuperinterface(kabinDaoInterface)
            .addSuperinterface(superClassName)

        val daoQueriesPropertyName = KabinDao<*>::queries.name
        val constructorBuilder = FunSpec.constructorBuilder()
            .addParameter(daoQueriesPropertyName, daoQueriesClassName)
            .build()

        val daoQueriesPropertySpec = PropertySpec.builder(
            daoQueriesPropertyName,
            daoQueriesClassName,
            KModifier.OVERRIDE
        ).initializer(daoQueriesPropertyName).build()

        classBuilder
            .primaryConstructor(constructorBuilder)
            .addProperty(daoQueriesPropertySpec)

        if (daoSpec.declaration.classKind == ClassKind.INTERFACE) {
        } else {
            classBuilder.superclass(superClassName)
        }

        daoSpec.functionSpecs.forEach { functionSpec ->
            val functionName = functionSpec.declaration.simpleName.asString()
            val parameters = functionSpec.parameters.joinToString { parameter ->
                parameter.name
            }

            val functionCodeBuilder = CodeBlock.builder()

            val returnType = functionSpec.returnType
            if (functionSpec.transactionSpec != null) {
                if (returnType != null) {
                    functionCodeBuilder.beginControlFlow("return transactionWithResult")
                } else {
                    functionCodeBuilder.beginControlFlow("transaction")
                }
            }


            val awaitFunction = when (returnType) {
                is TypeDeclaration.Class,
                is TypeDeclaration.Entity -> "awaitAsOneNotNullIO"
                is TypeDeclaration.EntityList -> "awaitAsListIO"
                null -> null
            }

            val functionCall = when (functionSpec.actionSpec) {
                is DaoActionSpec.Delete,
                is DaoActionSpec.Insert,
                is DaoActionSpec.Update,
                is DaoActionSpec.Upsert -> "$daoQueriesPropertyName.$functionName($parameters)"
                is DaoActionSpec.Query,
                is DaoActionSpec.RawQuery -> "$daoQueriesPropertyName.$functionName($parameters).$awaitFunction()"
                null -> "super.$functionName($parameters)"
            }

            val actualFunctionCall = if (returnType != null && functionSpec.transactionSpec == null) {
                "return $functionCall"
            } else {
                functionCall
            }

            functionCodeBuilder.addStatement(actualFunctionCall)

            if (functionSpec.transactionSpec != null) {
                functionCodeBuilder.endControlFlow()
            }

            val functionBuilder = functionSpec.declaration.buildSpec()
                .addModifiers(KModifier.OVERRIDE)
                .addCode(functionCodeBuilder.build())
                .build()

            classBuilder.addFunction(functionBuilder)
        }

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

    private fun generateDaoQueries(daoSpec: DaoSpec) {
        val daoQueriesFilePackage = daoSpec.declaration.packageName.asString()
        val daoQueriesFileName = buildString {
            append(daoSpec.declaration.simpleName.asString())
            append(options.getOrDefault(KabinOptions.Key.DAO_QUERIES_SUFFIX))
        }

        val className = ClassName(daoQueriesFilePackage, daoQueriesFileName)
        val superClassName = SuspendingTransacterImpl::class.asClassName()

        val classBuilder = TypeSpec.classBuilder(className)
            .superclass(superClassName)
            .addSuperclassConstructorParameter(DRIVER_NAME)

        val adapters = HashSet<ColumnAdapterReference>()
        val mappers = HashSet<MapperReference>()
        daoSpec.functionSpecs.forEach { functionSpec ->
            if (functionSpec.actionSpec != null) {
                val functionAdapters = classBuilder.addQueryFunction(functionSpec)
                adapters.addAll(functionAdapters.first)
                mappers.addAll(functionAdapters.second)
            }
        }

        val constructorBuilder = FunSpec.constructorBuilder()
            .addParameter(DRIVER_NAME, SqlDriver::class.asClassName())

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
                propertyName,
                adapterType
            )
        }

        mappers.forEach { mapper ->
            val propertyName = mapper.getPropertyName()
            val adapterType = KabinTable.Mapper::class.asClassName()
                .parameterizedBy(mapper.entityType)

            val propertySpec = PropertySpec.builder(
                propertyName,
                adapterType,
                KModifier.PRIVATE
            ).initializer(propertyName).build()

            classBuilder.addProperty(propertySpec)

            constructorBuilder.addParameter(
                propertyName,
                adapterType
            )
        }

        classBuilder.primaryConstructor(constructorBuilder.build())

        val classSpec = classBuilder.build()
        val fileSpec = FileSpec.builder(daoQueriesFilePackage, daoQueriesFileName)
            .addType(classSpec)
            .build()

        val outputFile = codeGenerator.createNewFile(
            Dependencies(aggregating = false),
            daoQueriesFilePackage,
            daoQueriesFileName
        )

        fileSpec.writeToFile(outputFile)
    }
}