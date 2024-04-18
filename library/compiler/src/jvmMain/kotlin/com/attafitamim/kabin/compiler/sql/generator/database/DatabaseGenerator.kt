package com.attafitamim.kabin.compiler.sql.generator.database

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlSchema
import com.attafitamim.kabin.compiler.sql.generator.dao.DaoGenerator
import com.attafitamim.kabin.compiler.sql.generator.mapper.MapperGenerator
import com.attafitamim.kabin.compiler.sql.generator.queries.QueriesGenerator
import com.attafitamim.kabin.compiler.sql.generator.references.ColumnAdapterReference
import com.attafitamim.kabin.compiler.sql.generator.references.MapperReference
import com.attafitamim.kabin.compiler.sql.generator.tables.TableGenerator
import com.attafitamim.kabin.compiler.sql.utils.poet.SCHEMA_CREATOR_NAME
import com.attafitamim.kabin.compiler.sql.utils.poet.SCHEMA_NAME
import com.attafitamim.kabin.compiler.sql.utils.poet.asPropertyName
import com.attafitamim.kabin.compiler.sql.utils.poet.buildSpec
import com.attafitamim.kabin.compiler.sql.utils.poet.parameterBuildSpec
import com.attafitamim.kabin.compiler.sql.utils.poet.references.asPropertyName
import com.attafitamim.kabin.compiler.sql.utils.poet.references.getClassName
import com.attafitamim.kabin.compiler.sql.utils.poet.references.getPropertyName
import com.attafitamim.kabin.compiler.sql.utils.poet.simpleNameString
import com.attafitamim.kabin.compiler.sql.utils.poet.typeInitializer
import com.attafitamim.kabin.compiler.sql.utils.poet.writeFile
import com.attafitamim.kabin.compiler.sql.utils.spec.converterSpecsByReferences
import com.attafitamim.kabin.compiler.sql.utils.spec.defaultAdapters
import com.attafitamim.kabin.compiler.sql.utils.spec.defaultMappers
import com.attafitamim.kabin.compiler.sql.utils.spec.getDatabaseClassName
import com.attafitamim.kabin.compiler.sql.utils.spec.getQueryFunctionName
import com.attafitamim.kabin.compiler.sql.utils.spec.mapperResultByReferences
import com.attafitamim.kabin.compiler.sql.utils.spec.mapperSpecsByReferences
import com.attafitamim.kabin.core.database.KabinBaseDatabase
import com.attafitamim.kabin.core.database.KabinSqlSchema
import com.attafitamim.kabin.core.table.KabinMapper
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.database.DatabaseSpec
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

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
        val generatedTables = LinkedHashSet<TableGenerator.Result>()
        val generatedMappers = LinkedHashSet<MapperGenerator.Result>()
        val generatedQueries = LinkedHashSet<QueriesGenerator.Result>()
        val generatedDaos = LinkedHashSet<DaoGenerator.Result>()
        val requiredAdapters = LinkedHashSet<ColumnAdapterReference>()
        val requiredMappers = LinkedHashSet<MapperReference>()

        databaseSpec.entities.forEach { entitySpec ->
            val tableResult = tableGenerator.generate(entitySpec)
            generatedTables.add(tableResult)

            val mapperResult = mapperGenerator.generate(entitySpec)
            generatedMappers.add(mapperResult)

            requiredAdapters.addAll(mapperResult.adapters)
        }

        databaseSpec.daoGetters.forEach { databaseDaoGetterSpec ->
            val daoSpec = databaseDaoGetterSpec.daoSpec

            val queriesResult = queriesGenerator.generate(daoSpec)
            generatedQueries.add(queriesResult)
            requiredAdapters.addAll(queriesResult.adapters)
            requiredMappers.addAll(queriesResult.mappers)

            val daoResult = daoGenerator.generate(daoSpec)
            generatedDaos.add(daoResult)
            requiredAdapters.addAll(daoResult.adapters)
        }

        generateDatabase(
            databaseSpec,
            generatedTables.toList(),
            generatedMappers.toList(),
            generatedQueries.toList(),
            generatedDaos.toList(),
            requiredAdapters.toList(),
            requiredMappers.toList()
        )
    }

    private fun generateDatabase(
        databaseSpec: DatabaseSpec,
        generatedTables: List<TableGenerator.Result>,
        generatedMappers: List<MapperGenerator.Result>,
        generatedQueries: List<QueriesGenerator.Result>,
        generatedDaos: List<DaoGenerator.Result>,
        requiredAdapters: List<ColumnAdapterReference>,
        requiredMappers: List<MapperReference>
    ) {
        val className = databaseSpec.getDatabaseClassName(options)
        val superClass = KabinBaseDatabase::class.asClassName()
        val databaseInterface = databaseSpec.declaration.toClassName()

        val classBuilder = TypeSpec.classBuilder(className)
            .superclass(superClass)
            .addSuperinterface(databaseInterface)
            .addModifiers(KModifier.PRIVATE)

        val driverName = KabinBaseDatabase::driver.name
        val driverType = KabinBaseDatabase::driver.returnType.asTypeName()
        val driverParameter = ParameterSpec.builder(
            driverName,
            driverType
        )

        val configurationName = KabinBaseDatabase::configuration.name
        val configurationType = KabinBaseDatabase::configuration.returnType.asTypeName()
        val configurationParameter = ParameterSpec.builder(
            configurationName,
            configurationType
        )

        val primaryConstructor = requireNotNull(KabinBaseDatabase::class.primaryConstructor)
        val primaryConstructorBuilder = FunSpec.constructorBuilder()
        primaryConstructor.parameters.forEach { kParameter ->
            primaryConstructorBuilder.addParameter(kParameter.buildSpec().build())
            classBuilder.addSuperclassConstructorParameter(requireNotNull(kParameter.name))
        }

        classBuilder.primaryConstructor(primaryConstructorBuilder.build())

        val typeConvertersMap = databaseSpec.typeConverters?.converterSpecsByReferences()
        requiredAdapters.forEach { adapter ->
            val propertyName = adapter.getPropertyName()
            val typeConverterSpec = typeConvertersMap?.get(adapter)
            val adapterClassName = typeConverterSpec?.declaration?.toClassName()
            val actualAdapterClassName = adapterClassName
                ?: defaultAdapters[adapter]
                ?: classBuilder.generateAdapter(className, adapter)
                ?: logger.throwException(
                    "No type converter found for $adapter in converts: $typeConvertersMap and defaultAdapters: $defaultAdapters",
                    databaseSpec.declaration
                )

            val adapterType = ColumnAdapter::class.asClassName()
                .parameterizedBy(adapter.kotlinType, adapter.affinityType)

            val propertyBuilder = PropertySpec.builder(
                propertyName,
                adapterType,
                KModifier.PRIVATE
            ).initializer("%T", actualAdapterClassName)

            classBuilder.addProperty(propertyBuilder.build())
        }

        val generatedMappersMap = generatedMappers.mapperResultByReferences()
        val providedMappers = databaseSpec.mappers
            ?.mapperSpecsByReferences()
            .orEmpty()

        requiredMappers.forEach { mapper ->
            val propertyName = mapper.getPropertyName(options)
            val mapperClassName = KabinMapper::class.asClassName()
                .parameterizedBy(mapper.returnType)

            val propertyBuilder = PropertySpec.builder(
                propertyName,
                mapperClassName,
                KModifier.PRIVATE
            )

            when {
                generatedMappersMap.contains(mapper) -> {
                    val generatedMapper = generatedMappersMap.getValue(mapper)
                    val parameters = generatedMapper.adapters
                        .map(ColumnAdapterReference::getPropertyName)

                    propertyBuilder.initializer(
                        typeInitializer(parameters),
                        generatedMapper.className
                    )
                }

                providedMappers.contains(mapper) -> {
                    val mapperSpec = providedMappers.getValue(mapper)
                    propertyBuilder.initializer("%T", mapperSpec.declaration.toClassName())
                }

                defaultMappers.contains(mapper) -> {
                    propertyBuilder.initializer("%T", defaultMappers.getValue(mapper))
                }

                else -> logger.throwException(
                    "No mapper found for $mapper",
                    databaseSpec.declaration
                )
            }

            classBuilder.addProperty(propertyBuilder.build())
        }

        generatedQueries.forEach { generatedQuery ->
            val propertyName = generatedQuery.className.asPropertyName()
            val parameters = ArrayList<String>()
            parameters.add(driverName)

            generatedQuery.adapters.forEach { adapter ->
                parameters.add(adapter.getPropertyName())
            }

            generatedQuery.mappers.forEach { mapper ->
                parameters.add(mapper.getPropertyName(options))
            }

            val propertyBuilder = PropertySpec.builder(
                propertyName,
                generatedQuery.className,
                KModifier.PRIVATE
            ).initializer(typeInitializer(parameters), generatedQuery.className)

            classBuilder.addProperty(propertyBuilder.build())
        }

        databaseSpec.daoGetters.forEachIndexed { index, databaseDaoGetterSpec ->
            val generatedDao = generatedDaos[index]

            val parameters = ArrayList<String>()
            val queryClassName = databaseDaoGetterSpec.daoSpec.getQueryFunctionName(options)
            parameters.add(queryClassName.asPropertyName())
            parameters.add(configurationName)

            generatedDao.adapters.forEach { adapter ->
                parameters.add(adapter.getPropertyName())
            }

            val propertyBuilder = PropertySpec.builder(
                databaseDaoGetterSpec.declaration.simpleNameString,
                generatedDao.className,
                KModifier.OVERRIDE
            ).initializer(typeInitializer(parameters), generatedDao.className)

            classBuilder.addProperty(propertyBuilder.build())
        }

        val databaseKClassType = KClass::class.asClassName().parameterizedBy(databaseInterface)
        val objectClassName = ClassName(className.packageName, className.simpleName, SCHEMA_NAME)
        classBuilder.addTableActions(generatedTables)

        val schemaObject = createSchemaObjectSpec(objectClassName, generatedTables)
        classBuilder.addType(schemaObject)
        
        val migrationsParameter = KabinSqlSchema::migrations
            .parameterBuildSpec().defaultValue("emptyList()")

        val migrationStrategyParameter = KabinSqlSchema::migrationStrategy
            .parameterBuildSpec().defaultValue("KabinMigrationStrategy.STRICT")

        val versionParameter = KabinSqlSchema::version
            .parameterBuildSpec().defaultValue(databaseSpec.version.toString())

        val schemaConstructorParametersCall = requireNotNull(KabinSqlSchema::class.primaryConstructor)
            .parameters.joinToString { kParameter ->
                requireNotNull(kParameter.name)
            }

        val schemaQueryType = QueryResult.AsyncValue::class.asClassName()
            .parameterizedBy(Unit::class.asClassName())

        val schemaReturnType = SqlSchema::class.asClassName()
            .parameterizedBy(schemaQueryType)

        val schemaExtensionName = SCHEMA_CREATOR_NAME
        val schemaExtension = FunSpec.builder(schemaExtensionName)
            .returns(schemaReturnType)
            .receiver(databaseKClassType)
            .addParameter(migrationsParameter.build())
            .addParameter(migrationStrategyParameter.build())
            .addParameter(versionParameter.build())
            .addParameter(configurationParameter.build())
            .addStatement("return·%T($schemaConstructorParametersCall)", objectClassName)
            .build()

        val newInstanceName = Class<*>::newInstance.name
        val newInstanceExtension = FunSpec.builder(newInstanceName)
            .receiver(databaseKClassType)
            .returns(databaseInterface)
            .addParameter(driverParameter.build())
            .addParameter(configurationParameter.build())
            .addStatement("return·%T($driverName, $configurationName)", className)
            .build()

        val schemaParameterName = SCHEMA_NAME.asPropertyName()
        val newInstanceFullExtension = FunSpec.builder(Class<*>::newInstance.name)
            .receiver(databaseKClassType)
            .returns(databaseInterface)
            .addModifiers()
            .addParameter(migrationsParameter.build())
            .addParameter(migrationStrategyParameter.build())
            .addParameter(versionParameter.build())
            .addParameter(configurationParameter.build())
            .addStatement("val·$schemaParameterName·=·$schemaExtensionName($schemaConstructorParametersCall)")
            .addStatement("val·$driverName·=·$configurationName.createDriver($schemaParameterName)")
            .addStatement("return·$newInstanceName($driverName, $configurationName)")
            .build()

        val fileSpec = FileSpec.builder(className)
            .addFunction(schemaExtension)
            .addFunction(newInstanceExtension)
            .addFunction(newInstanceFullExtension)
            .addType(classBuilder.build())
            .addImport("com.attafitamim.kabin.core.driver", "createDriver")
            .build()

        codeGenerator.writeFile(
            className,
            fileSpec
        )
    }

    private fun TypeSpec.Builder.generateAdapter(
        databaseClassName: ClassName,
        adapter: ColumnAdapterReference
    ): ClassName? = when (adapter.kotlinTypeKind) {
        ClassKind.ENUM_ENTRY,
        ClassKind.ENUM_CLASS -> generateEnumAdapter(
            databaseClassName,
            adapter
        )

        ClassKind.INTERFACE,
        ClassKind.CLASS,
        ClassKind.OBJECT,
        ClassKind.ANNOTATION_CLASS -> null
    }

    private fun TypeSpec.Builder.generateEnumAdapter(
        databaseClassName: ClassName,
        adapter: ColumnAdapterReference
    ): ClassName {
        val adapterType = ColumnAdapter::class.asClassName()
            .parameterizedBy(adapter.kotlinType, adapter.affinityType)

        val className = ClassName(
            databaseClassName.packageName,
            databaseClassName.simpleName,
            adapter.getClassName()
        )

        val decodeParameterName = "databaseValue"
        val encodeParameterName = "value"
        val decodeFunction = FunSpec.builder(ColumnAdapter<Enum<*>, String>::decode.name)
            .addParameter(decodeParameterName, adapter.affinityType)
            .returns(adapter.kotlinType)
            .addModifiers(KModifier.OVERRIDE)
            .addStatement("return·enumValueOf($decodeParameterName)")
            .build()

        val encodeFunction = FunSpec.builder(ColumnAdapter<Enum<*>, String>::encode.name)
            .addParameter(encodeParameterName, adapter.kotlinType)
            .returns(adapter.affinityType)
            .addModifiers(KModifier.OVERRIDE)
            .addStatement("return·$encodeParameterName.name")
            .build()

        val adapterSpec = TypeSpec.objectBuilder(className)
            .addSuperinterface(adapterType)
            .addFunction(decodeFunction)
            .addFunction(encodeFunction)
            .build()

        addType(adapterSpec)
        return className
    }

    private fun TypeSpec.Builder.addTableActions(
        generatedTables: List<TableGenerator.Result>
    ) {
        val driverName = KabinBaseDatabase::driver.name
        val clearFunctionBuilder = KabinBaseDatabase::clearTables.buildSpec()
            .addModifiers(KModifier.OVERRIDE)

        generatedTables.forEach { generatedTable ->
            clearFunctionBuilder.addStatement("%T.clear($driverName)", generatedTable.className)
        }

        addFunction(clearFunctionBuilder.build())
    }

    private fun createSchemaObjectSpec(
        className: ClassName,
        generatedTables: List<TableGenerator.Result>
    ): TypeSpec {
        val classBuilder = TypeSpec.classBuilder(className)
        val superClassName = KabinSqlSchema::class.asClassName()
        classBuilder.superclass(superClassName)

        val primaryConstructor = requireNotNull(KabinSqlSchema::class.primaryConstructor)
        val primaryConstructorBuilder = FunSpec.constructorBuilder()
        primaryConstructor.parameters.forEach { kParameter ->
            primaryConstructorBuilder.addParameter(kParameter.buildSpec().build())
            classBuilder.addSuperclassConstructorParameter(requireNotNull(kParameter.name))
        }

        classBuilder.primaryConstructor(primaryConstructorBuilder.build())
        val dropFunctionBuilder = KabinSqlSchema::dropTables.buildSpec()
            .addModifiers(KModifier.OVERRIDE)

        val createFunction = KabinSqlSchema::createTables.buildSpec()
        val driverName = createFunction.parameters.first().name
        val createFunctionBuilder = createFunction
            .addModifiers(KModifier.OVERRIDE)

        generatedTables.forEach { generatedTable ->
            createFunction.addStatement("%T.create($driverName)", generatedTable.className)
            dropFunctionBuilder.addStatement("%T.drop($driverName)", generatedTable.className)
        }

        classBuilder.addFunction(createFunctionBuilder.build())
        classBuilder.addFunction(dropFunctionBuilder.build())

        return classBuilder.build()
    }
}
