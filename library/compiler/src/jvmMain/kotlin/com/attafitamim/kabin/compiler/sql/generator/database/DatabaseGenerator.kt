package com.attafitamim.kabin.compiler.sql.generator.database

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.attafitamim.kabin.compiler.sql.generator.dao.DaoGenerator
import com.attafitamim.kabin.compiler.sql.generator.mapper.MapperGenerator
import com.attafitamim.kabin.compiler.sql.generator.queries.QueriesGenerator
import com.attafitamim.kabin.compiler.sql.generator.tables.TableGenerator
import com.attafitamim.kabin.compiler.sql.utils.poet.DRIVER_NAME
import com.attafitamim.kabin.compiler.sql.utils.poet.references.getPropertyName
import com.attafitamim.kabin.compiler.sql.utils.poet.toLowerCamelCase
import com.attafitamim.kabin.compiler.sql.utils.poet.writeToFile
import com.attafitamim.kabin.compiler.sql.utils.poet.writeType
import com.attafitamim.kabin.compiler.sql.utils.spec.getQueryClassName
import com.attafitamim.kabin.core.database.KabinDatabase
import com.attafitamim.kabin.core.table.KabinEntityMapper
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.specs.database.DatabaseSpec
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

class DatabaseGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: KabinOptions
) {

    private val tableGenerator = TableGenerator(codeGenerator, logger, options)
    private val mapperGenerator = MapperGenerator(codeGenerator, logger, options)
    private val queriesGenerator = QueriesGenerator(codeGenerator, logger, options)
    private val daoGenerator = DaoGenerator(codeGenerator, logger, options)

    fun generate(databaseSpec: DatabaseSpec) {
        val databaseFilePackage = databaseSpec.declaration.packageName.asString()
        val databaseFileName = buildString {
            append(databaseSpec.declaration.simpleName.asString())
            append(options.getOrDefault(KabinOptions.Key.DATABASE_SUFFIX))
        }

        val className = ClassName(databaseFilePackage, databaseFileName)
        val superClassName = KabinDatabase::class.asClassName()

        val driverName = DRIVER_NAME
        val constructorBuilder = FunSpec.constructorBuilder()
            .addParameter(driverName, SqlDriver::class.asClassName())

        val classBuilder = TypeSpec.classBuilder(className)
            .addSuperinterface(superClassName)
            .primaryConstructor(constructorBuilder.build())

        databaseSpec.entities.forEach { entitySpec ->
            tableGenerator.generate(entitySpec)
            mapperGenerator.generate(entitySpec)
        }

        databaseSpec.daoGetterSpecs.forEach { databaseDaoGetterSpec ->
            val generateResult = queriesGenerator.generate(databaseDaoGetterSpec.daoSpec)

            val parameters = ArrayList<String>()
            generateResult.adapters.forEach { adapter ->
                val adapterClassName = ColumnAdapter::class.asClassName()
                    .parameterizedBy(adapter.kotlinType, adapter.affinityType)

                val propertyName = adapter.getPropertyName()
                val adapterPropertySpec = PropertySpec.builder(
                    propertyName,
                    adapterClassName,
                    KModifier.PRIVATE
                ).initializer("null").build()

                classBuilder.addProperty(adapterPropertySpec)
                parameters.add(propertyName)
            }

            generateResult.mappers.forEach { mapper ->
                val mapperClassName = KabinEntityMapper::class.asClassName()
                    .parameterizedBy(mapper.entityType)

                val propertyName = mapper.getPropertyName()
                val adapterPropertySpec = PropertySpec.builder(
                    propertyName,
                    mapperClassName,
                    KModifier.PRIVATE
                ).initializer("null").build()

                classBuilder.addProperty(adapterPropertySpec)
                parameters.add(propertyName)
            }

            val queryClassName = databaseDaoGetterSpec.daoSpec.getQueryClassName(options)
            val queryPropertySpec = PropertySpec.builder(
                queryClassName.simpleName.toLowerCamelCase(),
                queryClassName,
                KModifier.PRIVATE
            ).initializer("${queryClassName.simpleName}($driverName, ${parameters.joinToString()})").build()

            classBuilder.addProperty(queryPropertySpec)
            daoGenerator.generate(databaseDaoGetterSpec.daoSpec)
        }

        codeGenerator.writeType(
            className,
            classBuilder.build()
        )
    }
}
