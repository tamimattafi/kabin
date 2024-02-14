package com.attafitamim.kabin.processor.ksp

import com.attafitamim.kabin.annotations.database.Database
import com.attafitamim.kabin.processor.handler.KabinSpecHandler
import com.attafitamim.kabin.processor.utils.argumentsMap
import com.attafitamim.kabin.processor.utils.getArgument
import com.attafitamim.kabin.processor.utils.isSame
import com.attafitamim.kabin.processor.utils.requireArgument
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.database.DatabaseSpec
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid

class KabinVisitor(
    private val specHandler: KabinSpecHandler,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : KSVisitorVoid() {

    private val databaseAnnotation = Database::class

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        validateClass(classDeclaration)
        val databaseSpec = getDatabaseSpec(classDeclaration)
        validateDatabase(classDeclaration, databaseSpec)
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

    private fun getDatabaseSpec(classDeclaration: KSClassDeclaration): DatabaseSpec {
        val databaseAnnotation = classDeclaration
            .annotations
            .first { annotation ->
                annotation.isSame(databaseAnnotation)
            }

        val argumentsMap = databaseAnnotation.argumentsMap
        return DatabaseSpec(
            classDeclaration,
            argumentsMap.requireArgument(Database::entities.name),
            argumentsMap.getArgument(Database::views.name),
            argumentsMap.requireArgument(Database::version.name),
            argumentsMap.requireArgument(Database::exportScheme.name),
            argumentsMap.getArgument(Database::autoMigrations.name)
        )
    }

    private fun validateDatabase(classDeclaration: KSClassDeclaration, spec: DatabaseSpec) {
        if (spec.entities.isEmpty()) {
            logger.throwException(
                "Database ${classDeclaration.simpleName.asString()} must have at least one entity",
                classDeclaration
            )
        }

        logger.throwException("Spec: $spec")
    }
}
