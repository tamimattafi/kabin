package com.attafitamim.kabin.processor.ksp.visitor

import com.attafitamim.kabin.annotations.dao.Dao
import com.attafitamim.kabin.annotations.database.Database
import com.attafitamim.kabin.processor.handler.KabinSpecHandler
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.utils.requireAnnotationArgumentsMap
import com.attafitamim.kabin.processor.utils.getArgument
import com.attafitamim.kabin.processor.utils.isInstanceOf
import com.attafitamim.kabin.processor.utils.requireArgument
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.database.DatabaseDaoGetterSpec
import com.attafitamim.kabin.specs.database.DatabaseSpec
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid

class KabinDatabaseVisitor(
    private val specHandler: KabinSpecHandler,
    private val logger: KSPLogger,
    private val options: KabinOptions
) : KSVisitorVoid() {

    private val databaseAnnotation = Database::class
    private val daoAnnotation = Dao::class

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        validateClass(classDeclaration)
        val databaseSpec = getDatabaseSpec(classDeclaration)
        validateDatabase(classDeclaration, databaseSpec)
        specHandler.handleDatabaseSpec(databaseSpec)
    }

    private fun getDatabaseSpec(classDeclaration: KSClassDeclaration): DatabaseSpec {
        val argumentsMap = classDeclaration
            .requireAnnotationArgumentsMap(databaseAnnotation)

        val daoGetterSpecs = classDeclaration.getDeclaredFunctions()
            .toList()
            .mapNotNull(::getDaoGetterSpec)

        return with(argumentsMap) {
            DatabaseSpec(
                classDeclaration,
                getArgument(Database::entities.name),
                getArgument(Database::views.name),
                requireArgument(Database::version.name),
                getArgument(Database::exportScheme.name, Database.DEFAULT_EXPORT_SCHEME),
                getArgument(Database::autoMigrations.name),
                daoGetterSpecs
            )
        }
    }

    @OptIn(KspExperimental::class)
    private fun getDaoGetterSpec(functionDeclaration: KSFunctionDeclaration): DatabaseDaoGetterSpec? {
        val returnType = functionDeclaration.returnType ?: return null
        val returnTypeDeclaration = returnType.resolve().declaration

        val isDaoReturnType = returnTypeDeclaration.annotations.any { annotation ->
            annotation.isInstanceOf(daoAnnotation)
        }

        if (!isDaoReturnType) {
            return null
        }

        val daoDeclaration = returnTypeDeclaration as KSClassDeclaration
        return DatabaseDaoGetterSpec(
            functionDeclaration,
            daoDeclaration
        )
    }

    private fun validateClass(classDeclaration: KSClassDeclaration) {
        if (classDeclaration.classKind != ClassKind.CLASS) {
            logger.throwException(
                "Only classes can be annotated with @${databaseAnnotation.simpleName}",
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
