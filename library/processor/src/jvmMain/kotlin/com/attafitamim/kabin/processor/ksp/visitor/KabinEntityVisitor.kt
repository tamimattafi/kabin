package com.attafitamim.kabin.processor.ksp.visitor

import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.annotations.column.Ignore
import com.attafitamim.kabin.annotations.entity.Entity
import com.attafitamim.kabin.annotations.relation.ForeignKey
import com.attafitamim.kabin.annotations.index.Index
import com.attafitamim.kabin.annotations.index.PrimaryKey
import com.attafitamim.kabin.processor.handler.KabinSpecHandler
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.utils.argumentsMap
import com.attafitamim.kabin.processor.utils.getAnnotationArgumentsMap
import com.attafitamim.kabin.processor.utils.requireAnnotationArgumentsMap
import com.attafitamim.kabin.processor.utils.getArgument
import com.attafitamim.kabin.processor.utils.getEnumArgument
import com.attafitamim.kabin.processor.utils.getEnumsArgument
import com.attafitamim.kabin.processor.utils.requireArgument
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.column.IgnoreSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.attafitamim.kabin.specs.relation.ForeignKeySpec
import com.attafitamim.kabin.specs.index.IndexSpec
import com.attafitamim.kabin.specs.index.PrimaryKeySpec
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Modifier

class KabinEntityVisitor(
    private val specHandler: KabinSpecHandler,
    private val logger: KSPLogger,
    private val options: KabinOptions
): KSVisitorVoid() {

    private val entityAnnotation = Entity::class

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        validateClass(classDeclaration)
        val entitySpec = getEntitySpec(classDeclaration)
        specHandler.handleEntitySpec(entitySpec)
    }

    private fun getEntitySpec(classDeclaration: KSClassDeclaration): EntitySpec {
        val argumentsMap = classDeclaration
            .requireAnnotationArgumentsMap(entityAnnotation)

        val indices = argumentsMap
            .getArgument<List<KSAnnotation>>(Entity::indices.name)
            ?.map(::getIndexSpec)

        val foreignKeys = argumentsMap
            .getArgument<List<KSAnnotation>>(Entity::foreignKeys.name)
            ?.map(::getForeignKeySpec)

        val columns = classDeclaration.getDeclaredProperties()
            .toList()
            .map(::getColumnSpec)

        val name = argumentsMap
            .getArgument(Entity::tableName.name, Entity.DEFAULT_TABLE_NAME)
            .takeIf(String::isNotBlank)

        val inheritSuperIndices = argumentsMap
            .getArgument(Entity::inheritSuperIndices.name, Entity.DEFAULT_INHERIT_SUPER_INDICES)

        return EntitySpec(
            classDeclaration,
            name,
            indices,
            inheritSuperIndices,
            argumentsMap.getArgument(Entity::primaryKeys.name),
            foreignKeys,
            argumentsMap.getArgument(Entity::ignoredColumns.name),
            columns
        )
    }

    private fun getIndexSpec(annotation: KSAnnotation): IndexSpec =
        with(annotation.argumentsMap) {
            IndexSpec(
                getArgument(Index::columns.name),
                getEnumsArgument(Index::orders.name),
                getArgument(Index::name.name, Index.DEFAULT_NAME).takeIf(String::isNotBlank),
                getArgument(Index::unique.name, Index.DEFAULT_UNIQUE)
            )
        }

    private fun getForeignKeySpec(annotation: KSAnnotation): ForeignKeySpec =
        with(annotation.argumentsMap) {
            ForeignKeySpec(
                requireArgument(ForeignKey::entity.name),
                getArgument(ForeignKey::parentColumns.name),
                getArgument(ForeignKey::childColumns.name),
                getEnumArgument<ForeignKey.Action>(ForeignKey::onDelete.name),
                getEnumArgument<ForeignKey.Action>(ForeignKey::onUpdate.name),
                getArgument(ForeignKey::deferred.name, ForeignKey.DEFAULT_DEFERRED),
            )
        }

    private fun getColumnSpec(property: KSPropertyDeclaration): ColumnSpec {
        val primaryKeySpec = property
            .getAnnotationArgumentsMap(PrimaryKey::class)
            ?.run {
                PrimaryKeySpec(
                    getArgument(PrimaryKey::autoGenerate.name, PrimaryKey.DEFAULT_AUTO_GENERATE)
                )
            }

        val ignoreSpec = property
            .getAnnotationArgumentsMap(Ignore::class)
            ?.run {
                IgnoreSpec
            }

        val columnInfoArguments = property
            .getAnnotationArgumentsMap(ColumnInfo::class)
            .orEmpty()

        val name = columnInfoArguments
            .getArgument(ColumnInfo::name.name, ColumnInfo.DEFAULT_COLUMN_NAME)
            .takeIf(String::isNotBlank)

        val defaultValue = columnInfoArguments
            .getArgument(ColumnInfo::defaultValue.name, ColumnInfo.DEFAULT_VALUE)
            .takeIf(String::isNotBlank)

        return with(columnInfoArguments) {
            ColumnSpec(
                property,
                name,
                getArgument(ColumnInfo::typeAffinity.name),
                getArgument(ColumnInfo::index.name, ColumnInfo.DEFAULT_INDEX),
                getArgument(ColumnInfo::collate.name),
                defaultValue,
                primaryKeySpec,
                ignoreSpec
            )
        }
    }

    private fun validateClass(classDeclaration: KSClassDeclaration) {
        if (classDeclaration.classKind != ClassKind.CLASS) {
            logger.throwException(
                "Only classes can be annotated with @${entityAnnotation.simpleName}",
                classDeclaration
            )
        }

        if (!classDeclaration.modifiers.contains(Modifier.DATA)) {
            logger.throwException(
                "Entities annotated with @${entityAnnotation.simpleName} must be data classes",
                classDeclaration
            )
        }
    }
}