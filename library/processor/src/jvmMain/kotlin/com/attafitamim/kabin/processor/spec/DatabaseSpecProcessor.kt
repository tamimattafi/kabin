package com.attafitamim.kabin.processor.spec

import com.attafitamim.kabin.annotations.dao.Dao
import com.attafitamim.kabin.annotations.database.Database
import com.attafitamim.kabin.processor.utils.getArgument
import com.attafitamim.kabin.processor.utils.getClassDeclaration
import com.attafitamim.kabin.processor.utils.getClassDeclarations
import com.attafitamim.kabin.processor.utils.isInstanceOf
import com.attafitamim.kabin.processor.utils.requireAnnotationArgumentsMap
import com.attafitamim.kabin.processor.utils.requireArgument
import com.attafitamim.kabin.processor.utils.requireClassDeclarations
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.database.DatabaseDaoGetterSpec
import com.attafitamim.kabin.specs.database.DatabaseSpec
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType

class DatabaseSpecProcessor(private val logger: KSPLogger) {

    private val databaseAnnotation = Database::class
    private val daoAnnotation = Dao::class

    private val entitySpecProcessor = EntitySpecProcessor(logger)
    private val daoSpecProcessor = DaoSpecProcessor(logger)

    fun getDatabaseSpec(classDeclaration: KSClassDeclaration): DatabaseSpec {
        validateClass(classDeclaration)

        val argumentsMap = classDeclaration
            .requireAnnotationArgumentsMap(databaseAnnotation)

        val daoGetterSpecs = classDeclaration.getDeclaredProperties()
            .toList()
            .mapNotNull(::getDaoGetterSpec)

        val entitySpecs = argumentsMap
            .requireClassDeclarations(Database::entities.name)
            .map(entitySpecProcessor::getEntitySpec)

        val databaseSpec = with(argumentsMap) {
            DatabaseSpec(
                classDeclaration,
                entitySpecs,
                getArgument(Database::views.name),
                requireArgument(Database::version.name),
                getArgument(Database::exportScheme.name, Database.DEFAULT_EXPORT_SCHEME),
                getArgument(Database::autoMigrations.name),
                daoGetterSpecs
            )
        }

        validateDatabase(classDeclaration, databaseSpec)
        return databaseSpec
    }

    private fun getDaoGetterSpec(propertyDeclaration: KSPropertyDeclaration): DatabaseDaoGetterSpec? {
        val returnType = propertyDeclaration.type
        val returnTypeDeclaration = returnType.resolve().declaration

        val isDaoReturnType = returnTypeDeclaration.annotations.any { annotation ->
            annotation.isInstanceOf(daoAnnotation)
        }

        if (!isDaoReturnType) {
            return null
        }

        val daoDeclaration = returnTypeDeclaration as KSClassDeclaration
        val daoSpec = daoSpecProcessor.getDaoSpec(daoDeclaration)

        return DatabaseDaoGetterSpec(
            propertyDeclaration,
            daoSpec
        )
    }

    private fun validateClass(classDeclaration: KSClassDeclaration) {
        if (classDeclaration.classKind != ClassKind.INTERFACE) {
            logger.throwException(
                "Only interface can be annotated with @${databaseAnnotation.simpleName}",
                classDeclaration
            )
        }

        if (!classDeclaration.isAbstract()) {
            logger.throwException(
                "Classes annotated with @${databaseAnnotation.simpleName} must be abstract",
                classDeclaration
            )
        }
    }

    private fun validateDatabase(classDeclaration: KSClassDeclaration, spec: DatabaseSpec) {
        if (spec.entities.isNullOrEmpty()) {
            logger.throwException(
                "Database ${classDeclaration.simpleName.asString()} must have at least one entity",
                classDeclaration
            )
        }
    }
}