package com.attafitamim.kabin.processor.spec

import com.attafitamim.kabin.annotations.converters.Mappers
import com.attafitamim.kabin.annotations.converters.TypeConverters
import com.attafitamim.kabin.annotations.dao.Dao
import com.attafitamim.kabin.annotations.database.Database
import com.attafitamim.kabin.processor.utils.getAnnotationArgumentsMap
import com.attafitamim.kabin.processor.utils.getArgument
import com.attafitamim.kabin.processor.utils.isInstanceOf
import com.attafitamim.kabin.processor.utils.requireAnnotationArgumentsMap
import com.attafitamim.kabin.processor.utils.requireArgument
import com.attafitamim.kabin.processor.utils.requireClassDeclarations
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.database.DatabaseDaoGetterSpec
import com.attafitamim.kabin.specs.database.DatabaseSpec
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class DatabaseSpecProcessor(private val logger: KSPLogger) {

    private val databaseAnnotation = Database::class
    private val daoAnnotation = Dao::class
    private val typeConvertersAnnotation = TypeConverters::class
    private val mappersAnnotation = Mappers::class

    private val entitySpecProcessor = EntitySpecProcessor(logger)
    private val daoSpecProcessor = DaoSpecProcessor(logger)
    private val typeConverterSpecProcessor = TypeConverterSpecProcessor(logger)
    private val mapperSpecProcessor = MapperSpecProcessor(logger)

    fun getDatabaseSpec(classDeclaration: KSClassDeclaration): DatabaseSpec {
        validateClass(classDeclaration)

        val argumentsMap = classDeclaration
            .requireAnnotationArgumentsMap(databaseAnnotation)

        val typeConvertersArgumentsMap = classDeclaration
            .getAnnotationArgumentsMap(typeConvertersAnnotation)

        val mappersArgumentsMap = classDeclaration
            .getAnnotationArgumentsMap(mappersAnnotation)

        val daoGetterSpecs = classDeclaration.getDeclaredProperties()
            .toList()
            .mapNotNull(::getDaoGetterSpec)

        val entitySpecs = argumentsMap
            .requireClassDeclarations(Database::entities.name)
            .map(entitySpecProcessor::getEntitySpec)

        val typeConverterSpecs = typeConvertersArgumentsMap
            ?.requireClassDeclarations(TypeConverters::value.name)
            ?.map(typeConverterSpecProcessor::getTypeConverterSpec)

        val mapperSpecs = mappersArgumentsMap
            ?.requireClassDeclarations(Mappers::value.name)
            ?.map(mapperSpecProcessor::getTypeConverterSpec)

        val databaseSpec = with(argumentsMap) {
            DatabaseSpec(
                classDeclaration,
                entitySpecs,
                getArgument(Database::views.name),
                requireArgument(Database::version.name),
                getArgument(Database::exportSchema.name, Database.DEFAULT_EXPORT_SCHEMA),
                getArgument(Database::autoMigrations.name),
                daoGetterSpecs,
                typeConverterSpecs,
                mapperSpecs
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
        if (spec.entities.isEmpty()) {
            logger.throwException(
                "Database ${classDeclaration.simpleName.asString()} must have at least one entity",
                classDeclaration
            )
        }
    }
}