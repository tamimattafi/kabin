package com.attafitamim.kabin.compiler.sql.generator.tables

import com.attafitamim.kabin.compiler.sql.utils.poet.buildSpec
import com.attafitamim.kabin.compiler.sql.utils.poet.sqldelight.addDriverExecutionCode
import com.attafitamim.kabin.compiler.sql.utils.poet.writeToFile
import com.attafitamim.kabin.compiler.sql.utils.poet.writeType
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.getIndicesCreationQueries
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.tableClearQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.tableCreationQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.tableDropQuery
import com.attafitamim.kabin.core.table.KabinTable
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName

class TableGenerator(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: KabinOptions
) {

    fun generate(entitySpec: EntitySpec): Result {
        val tableFilePackage = entitySpec.declaration.packageName.asString()
        val tableFileName = buildString {
            append(entitySpec.declaration.simpleName.asString())
            append(options.getOrDefault(KabinOptions.Key.TABLE_SUFFIX))
        }

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

        val className = ClassName(tableFilePackage, tableFileName)
        val superClassName = KabinTable::class.asClassName()

        val classBuilder = TypeSpec.objectBuilder(className)
            .addSuperinterface(superClassName)
            .addFunction(createFunctionSpec)
            .addFunction(dropFunctionSpec)
            .addFunction(clearFunctionSpec)

        codeGenerator.writeType(
            className,
            classBuilder.build()
        )

        return Result(className)
    }

    data class Result(
        val className: ClassName
    )
}