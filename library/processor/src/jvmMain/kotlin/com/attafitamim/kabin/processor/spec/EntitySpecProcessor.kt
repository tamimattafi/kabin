package com.attafitamim.kabin.processor.spec

import com.attafitamim.kabin.annotations.ColumnInfo
import com.attafitamim.kabin.annotations.Ignore
import com.attafitamim.kabin.annotations.Embedded
import com.attafitamim.kabin.annotations.Entity
import com.attafitamim.kabin.annotations.Fts4
import com.attafitamim.kabin.annotations.Index
import com.attafitamim.kabin.annotations.PrimaryKey
import com.attafitamim.kabin.annotations.ForeignKey
import com.attafitamim.kabin.processor.utils.argumentsMap
import com.attafitamim.kabin.processor.utils.classDeclaration
import com.attafitamim.kabin.processor.utils.getAnnotationArgumentsMap
import com.attafitamim.kabin.processor.utils.getArgument
import com.attafitamim.kabin.processor.utils.getEnumArgument
import com.attafitamim.kabin.processor.utils.getEnumsArgument
import com.attafitamim.kabin.processor.utils.requireAnnotationArgumentsMap
import com.attafitamim.kabin.processor.utils.requireArgument
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.column.ColumnTypeSpec
import com.attafitamim.kabin.specs.column.IgnoreSpec
import com.attafitamim.kabin.specs.entity.EntitySearchSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.attafitamim.kabin.specs.index.IndexSpec
import com.attafitamim.kabin.specs.index.PrimaryKeySpec
import com.attafitamim.kabin.specs.relation.ForeignKeySpec
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSValueArgument
import com.google.devtools.ksp.symbol.Modifier

class EntitySpecProcessor(private val logger: KSPLogger) {

    private val entityAnnotation = Entity::class
    private val fts4Annotation = Fts4::class

    fun getEntitySpec(classDeclaration: KSClassDeclaration): EntitySpec {
        validateClass(classDeclaration)
        val entity = try {
            val argumentsMap = classDeclaration
                .requireAnnotationArgumentsMap(entityAnnotation)

            val indices = argumentsMap
                .getArgument<List<KSAnnotation>>(Entity::indices.name)
                ?.map(::getIndexSpec)

            val foreignKeys = argumentsMap
                .getArgument<List<KSAnnotation>>(Entity::foreignKeys.name)
                ?.map { annotation ->
                    getForeignKeySpec(classDeclaration, annotation)
                }

            val primaryKeys = argumentsMap.getArgument<List<String>>(Entity::primaryKeys.name)
            val ignoredColumns = argumentsMap.getArgument<List<String>>(Entity::ignoredColumns.name)

            val primaryKeysSet = LinkedHashSet(primaryKeys.orEmpty())
            val ignoredColumnsSet = LinkedHashSet(ignoredColumns.orEmpty())

            val columns = classDeclaration.getDeclaredProperties()
                .toList()
                .mapNotNull { propertyDeclaration ->
                    getColumnSpec(
                        propertyDeclaration,
                        primaryKeysSet,
                        ignoredColumnsSet
                    )
                }

            val name = argumentsMap
                .getArgument(Entity::tableName.name, Entity.DEFAULT_TABLE_NAME)
                .takeIf(String::isNotBlank) ?: classDeclaration.simpleName.asString()

            val inheritSuperIndices = argumentsMap
                .getArgument(Entity::inheritSuperIndices.name, Entity.DEFAULT_INHERIT_SUPER_INDICES)

            val searchSpec = getSearchSpec(classDeclaration)

            EntitySpec(
                classDeclaration,
                name,
                indices,
                inheritSuperIndices,
                primaryKeysSet,
                foreignKeys,
                ignoredColumnsSet,
                columns,
                searchSpec
            )
        } catch (exception: Exception) {
            logger.throwException(
                "Something went wrong while trying to process entity: $exception",
                classDeclaration
            )
        }

        validateEntity(entity)
        return entity
    }

    @OptIn(KspExperimental::class)
    fun hasEntityAnnotation(classDeclaration: KSClassDeclaration) =
        classDeclaration.isAnnotationPresent(entityAnnotation)

    private fun getIndexSpec(annotation: KSAnnotation): IndexSpec =
        with(annotation.argumentsMap) {
            IndexSpec(
                getArgument(Index::columns.name),
                getEnumsArgument(Index::orders.name),
                getArgument(Index::name.name, Index.DEFAULT_NAME).takeIf(String::isNotBlank),
                getArgument(Index::unique.name, Index.DEFAULT_UNIQUE)
            )
        }

    private fun getForeignKeySpec(
        parentDeclaration: KSClassDeclaration,
        annotation: KSAnnotation,
    ): ForeignKeySpec =
        with(annotation.argumentsMap) {
            val entityDeclaration = requireArgument<KSType>(ForeignKey::entity.name)
                .classDeclaration

            if (parentDeclaration == entityDeclaration) {
                logger.throwException(
                    "Foreign keys can't reference the same table",
                    parentDeclaration
                )
            }

            ForeignKeySpec(
                getEntitySpec(entityDeclaration),
                getArgument(ForeignKey::parentColumns.name),
                getArgument(ForeignKey::childColumns.name),
                getEnumArgument<ForeignKey.Action>(ForeignKey::onDelete.name),
                getEnumArgument<ForeignKey.Action>(ForeignKey::onUpdate.name),
                getArgument(ForeignKey::deferred.name, ForeignKey.DEFAULT_DEFERRED),
            )
        }

    private fun getColumnSpec(
        property: KSPropertyDeclaration,
        primaryKeysSet: MutableSet<String>,
        ignoredColumnsSet: MutableSet<String>,
        parentPrimaryKeySpec: PrimaryKeySpec? = null,
        isNullable: Boolean = false,
        prefix: String? = null
    ): ColumnSpec? {
        val columnInfoArguments = property
            .getAnnotationArgumentsMap(ColumnInfo::class)
            .orEmpty()

        val name = columnInfoArguments
            .getArgument(ColumnInfo::name.name, ColumnInfo.DEFAULT_COLUMN_NAME)
            .takeIf(String::isNotBlank) ?: property.simpleName.asString()

        val actualName = if (prefix.isNullOrBlank()) {
            name
        } else buildString {
            append(prefix, name)
        }

        if (ignoredColumnsSet.contains(actualName)) {
            return null
        }

        val ignoreSpec = property
            .getAnnotationArgumentsMap(Ignore::class)
            ?.run {
                IgnoreSpec
            }

        if (ignoreSpec != null) {
            ignoredColumnsSet.add(actualName)
            return null
        }

        val embeddedArguments = property
            .getAnnotationArgumentsMap(Embedded::class)

        val primaryKeySpec = getPrimaryKeySpec(
            actualName,
            embeddedArguments,
            property,
            primaryKeysSet,
            parentPrimaryKeySpec
        )

        val defaultValue = columnInfoArguments
            .getArgument(ColumnInfo::defaultValue.name, ColumnInfo.DEFAULT_VALUE)
            .takeIf(String::isNotBlank)

        val typeSpec = getColumnTypeSpec(
            property,
            primaryKeysSet,
            ignoredColumnsSet,
            embeddedArguments,
            primaryKeySpec,
            isNullable,
            prefix
        )

        return with(columnInfoArguments) {
            ColumnSpec(
                property,
                actualName,
                getEnumArgument<ColumnInfo.TypeAffinity>(ColumnInfo::typeAffinity.name),
                getArgument(ColumnInfo::index.name, ColumnInfo.DEFAULT_INDEX),
                getEnumArgument<ColumnInfo.Collate>(ColumnInfo::collate.name),
                defaultValue,
                primaryKeySpec,
                typeSpec
            )
        }
    }

    private fun getPrimaryKeySpec(
        columnName: String,
        embeddedArguments: Map<String, KSValueArgument>?,
        property: KSPropertyDeclaration,
        primaryKeysSet: MutableSet<String>,
        parentPrimaryKeySpec: PrimaryKeySpec?
    ): PrimaryKeySpec? {
        val primaryKeySpecArguments = property
            .getAnnotationArgumentsMap(PrimaryKey::class)

        val primaryKey = when {
            primaryKeySpecArguments != null -> {
                val authGenerate = primaryKeySpecArguments.getArgument(
                    PrimaryKey::autoGenerate.name,
                    PrimaryKey.DEFAULT_AUTO_GENERATE
                )

                PrimaryKeySpec(authGenerate)
            }

            parentPrimaryKeySpec != null -> parentPrimaryKeySpec
            primaryKeysSet.contains(columnName) -> {
                PrimaryKeySpec(PrimaryKey.DEFAULT_AUTO_GENERATE)
            }

            else -> null
        }

        if (embeddedArguments == null && primaryKey != null) {
            primaryKeysSet.add(columnName)
        }

        return primaryKey
    }

    private fun getColumnTypeSpec(
        property: KSPropertyDeclaration,
        primaryKeysSet: MutableSet<String>,
        ignoredColumnsSet: MutableSet<String>,
        embeddedArguments: Map<String, KSValueArgument>?,
        primaryKeySpec: PrimaryKeySpec?,
        isNullable: Boolean,
        prefix: String?
    ): ColumnTypeSpec {
        val type = property.type.resolve()
        val classDeclaration = type.classDeclaration
        val isMarkedNullable = type.isMarkedNullable || isNullable

        val dataType = getColumnTypeSpecDataType(
            type,
            primaryKeysSet,
            ignoredColumnsSet,
            embeddedArguments,
            primaryKeySpec,
            isMarkedNullable,
            prefix
        )

        return ColumnTypeSpec(
            property.type,
            type,
            classDeclaration,
            isMarkedNullable,
            dataType
        )
    }

    private fun getColumnTypeSpecDataType(
        type: KSType,
        primaryKeysSet: MutableSet<String>,
        ignoredColumnsSet: MutableSet<String>,
        embeddedArguments: Map<String, KSValueArgument>?,
        primaryKeySpec: PrimaryKeySpec?,
        isNullable: Boolean,
        prefix: String?
    ): ColumnTypeSpec.DataType {
        return if (embeddedArguments.isNullOrEmpty()) {
            ColumnTypeSpec.DataType.Class
        } else {
            val newPrefix = embeddedArguments.getArgument(
                Embedded::prefix.name,
                Embedded.DEFAULT_PREFIX
            ).takeIf(String::isNotBlank)

            val actualPrefix = if (newPrefix.isNullOrBlank()) {
                prefix
            } else if (prefix.isNullOrBlank()) {
                newPrefix
            } else buildString {
                append(prefix, newPrefix)
            }

            val columns = type.classDeclaration.getDeclaredProperties()
                .toList()
                .mapNotNull { propertyDeclaration ->
                    getColumnSpec(
                        propertyDeclaration,
                        primaryKeysSet,
                        ignoredColumnsSet,
                        primaryKeySpec,
                        isNullable,
                        actualPrefix
                    )
                }

            ColumnTypeSpec.DataType.Embedded(
                actualPrefix,
                columns
            )
        }
    }

    private fun getSearchSpec(classDeclaration: KSClassDeclaration): EntitySearchSpec? {
        val fts4Arguments = classDeclaration
            .getAnnotationArgumentsMap(fts4Annotation)
            ?: return null

        val entityDeclaration = fts4Arguments.requireArgument<KSType>(
            Fts4::contentEntity.name
        ).classDeclaration

        val entity = getEntitySpec(entityDeclaration)
        return EntitySearchSpec(entity)
    }

    private fun validateEntity(entitySpec: EntitySpec) {
        when {
            entitySpec.primaryKeys.isEmpty() && entitySpec.searchSpec == null -> logger.throwException(
                "An Entity must have at least one primary key",
                entitySpec.declaration
            )
        }
    }

    private fun validateClass(classDeclaration: KSClassDeclaration) {
        when {
            classDeclaration.classKind != ClassKind.CLASS -> logger.throwException(
                "Only classes can be annotated with @${entityAnnotation.simpleName}",
                classDeclaration
            )

            !classDeclaration.modifiers.contains(Modifier.DATA) -> logger.throwException(
                "Entities annotated with @${entityAnnotation.simpleName} must be data classes",
                classDeclaration
            )
        }
    }
}
