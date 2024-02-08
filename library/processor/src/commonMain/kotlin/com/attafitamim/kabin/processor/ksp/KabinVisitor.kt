package com.attafitamim.kabin.processor.ksp

import com.attafitamim.kabin.annotations.database.Database
import com.attafitamim.kabin.processor.utils.classSpec
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.processor.utils.toClassSpecs
import com.attafitamim.kabin.specs.database.DatabaseSpec
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid

class KabinVisitor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : KSVisitorVoid() {

    private val databaseAnnotation = Database::class

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val databaseSpec = getDatabaseSpec(classDeclaration)
        logger.logging("DATABASE_SPEC: $databaseSpec", classDeclaration)
    }

    private fun getDatabaseSpec(classDeclaration: KSClassDeclaration): DatabaseSpec {
        validateClass(classDeclaration)
        val databaseAnnotation = getDatabaseAnnotation(classDeclaration)
        validateDatabase(classDeclaration, databaseAnnotation)

        return DatabaseSpec(
            classDeclaration.classSpec,
            databaseAnnotation.entities.toClassSpecs(),
            databaseAnnotation.views.toClassSpecs(),
            databaseAnnotation.version,
            databaseAnnotation.autoMigrations.toList()
        )
    }

    private fun validateClass(classDeclaration: KSClassDeclaration) {
        if (classDeclaration.classKind != ClassKind.CLASS) {
            logger.throwException(
                "Only classes can be annotated with $databaseAnnotation",
                classDeclaration
            )
        }

        if (!classDeclaration.isAbstract()) {
            logger.throwException(
                "Database classes annotated with $databaseAnnotation must be abstract",
                classDeclaration
            )
        }
    }

    @OptIn(KspExperimental::class)
    private fun getDatabaseAnnotation(classDeclaration: KSClassDeclaration): Database {
        val databaseAnnotations = classDeclaration
            .getAnnotationsByType(Database::class)
            .toList()

        if (databaseAnnotations.size != 1) {
            logger.throwException(
                "Database classes must have exactly 1 annotation with type $databaseAnnotation",
                classDeclaration
            )
        }

        return databaseAnnotations.first()
    }

    private fun validateDatabase(classDeclaration: KSClassDeclaration, annotation: Database) {
        if (annotation.entities.isEmpty()) {
            logger.throwException(
                "Database ${classDeclaration.simpleName} must have at least one entity",
                classDeclaration
            )
        }
    }
}
