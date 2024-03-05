package com.attafitamim.kabin.compiler.sql.generator.database

import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import com.attafitamim.kabin.compiler.sql.generator.dao.DaoGenerator
import com.attafitamim.kabin.compiler.sql.generator.mapper.MapperGenerator
import com.attafitamim.kabin.compiler.sql.generator.queries.QueriesGenerator
import com.attafitamim.kabin.compiler.sql.generator.references.ColumnAdapterReference
import com.attafitamim.kabin.compiler.sql.generator.references.MapperReference
import com.attafitamim.kabin.compiler.sql.generator.tables.TableGenerator
import com.attafitamim.kabin.compiler.sql.utils.poet.DRIVER_NAME
import com.attafitamim.kabin.compiler.sql.utils.poet.SCHEME_NAME
import com.attafitamim.kabin.compiler.sql.utils.poet.asPropertyName
import com.attafitamim.kabin.compiler.sql.utils.poet.buildSpec
import com.attafitamim.kabin.compiler.sql.utils.poet.references.getPropertyName
import com.attafitamim.kabin.compiler.sql.utils.poet.toCamelCase
import com.attafitamim.kabin.compiler.sql.utils.poet.toPascalCase
import com.attafitamim.kabin.compiler.sql.utils.poet.typeInitializer
import com.attafitamim.kabin.compiler.sql.utils.poet.writeFile
import com.attafitamim.kabin.compiler.sql.utils.spec.converterSpecsByReferences
import com.attafitamim.kabin.compiler.sql.utils.spec.defaultAdapters
import com.attafitamim.kabin.compiler.sql.utils.spec.defaultMappers
import com.attafitamim.kabin.compiler.sql.utils.spec.getDaoClassName
import com.attafitamim.kabin.compiler.sql.utils.spec.getDatabaseClassName
import com.attafitamim.kabin.compiler.sql.utils.spec.getQueryClassName
import com.attafitamim.kabin.compiler.sql.utils.spec.mapperResultByReferences
import com.attafitamim.kabin.compiler.sql.utils.spec.mapperSpecsByReferences
import com.attafitamim.kabin.core.database.KabinDatabase
import com.attafitamim.kabin.core.table.KabinMapper
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.database.DatabaseSpec
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import kotlin.reflect.KClass

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

            daoGenerator.generate(daoSpec)
        }

        generateDatabase(
            databaseSpec,
            generatedTables,
            generatedMappers,
            generatedQueries,
            requiredAdapters,
            requiredMappers
        )
    }

    private fun generateDatabase(
        databaseSpec: DatabaseSpec,
        generatedTables: Set<TableGenerator.Result>,
        generatedMappers: Set<MapperGenerator.Result>,
        generatedQueries: Set<QueriesGenerator.Result>,
        requiredAdapters: Set<ColumnAdapterReference>,
        requiredMappers: Set<MapperReference>
    ) {
        val className = databaseSpec.getDatabaseClassName(options)
        val superInterface = KabinDatabase::class.asClassName()
        val databaseInterface = databaseSpec.declaration.toClassName()

        val classBuilder = TypeSpec.classBuilder(className)
            .addSuperinterface(superInterface)
            .addSuperinterface(databaseInterface)
            .addModifiers(KModifier.PRIVATE)

        val driverName = DRIVER_NAME
        val driverType = SqlDriver::class.asClassName()
        val constructorBuilder = FunSpec.constructorBuilder()
            .addParameter(driverName, SqlDriver::class.asClassName())

        classBuilder.primaryConstructor(constructorBuilder.build())

        val typeConvertersMap = databaseSpec.typeConverters?.converterSpecsByReferences()
        requiredAdapters.forEach { adapter ->
            val propertyName = adapter.getPropertyName()
            val typeConverterSpec = typeConvertersMap?.get(adapter)
            val adapterClassName = typeConverterSpec?.declaration?.toClassName()
            val actualAdapterClassName = adapterClassName
                ?: defaultAdapters[adapter]
                ?: classBuilder.generateAdapter(className, adapter)
                ?: logger.throwException(
                    "No type converter found for $adapter",
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

        databaseSpec.daoGetters.forEach { databaseDaoGetterSpec ->
            val queryClassName = databaseDaoGetterSpec.daoSpec.getQueryClassName(options)
            val daoClassName = databaseDaoGetterSpec.daoSpec.getDaoClassName(options)

            val parameters = listOf(queryClassName.asPropertyName())
            val propertyBuilder = PropertySpec.builder(
                databaseDaoGetterSpec.declaration.simpleName.asString(),
                daoClassName,
                KModifier.OVERRIDE
            ).initializer(typeInitializer(parameters), daoClassName)

            classBuilder.addProperty(propertyBuilder.build())
        }

        val databaseKClassType = KClass::class.asClassName().parameterizedBy(databaseInterface)
        val objectClassName = ClassName(className.packageName, className.simpleName, SCHEME_NAME)
        val schemeObject = createSchemeObjectSpec(objectClassName, databaseSpec, generatedTables)
        classBuilder.addType(schemeObject)

        val schemeGetter = FunSpec.getterBuilder().addStatement(
            "return %T",
            objectClassName
        )

        val schemeExtension = PropertySpec.builder(
            SCHEME_NAME.toCamelCase(),
            schemeObject.superinterfaces.entries.first().key
        ).receiver(databaseKClassType)
            .getter(schemeGetter.build())
            .build()

        val newInstanceExtension = FunSpec.builder(Class<*>::newInstance.name)
            .receiver(databaseKClassType)
            .returns(databaseInterface)
            .addParameter(driverName, driverType)
            .addStatement("return %T($driverName)", className)
            .build()

        val fileSpec = FileSpec.builder(className)
            .addProperty(schemeExtension)
            .addFunction(newInstanceExtension)
            .addType(classBuilder.build())
            .build()

        codeGenerator.writeFile(
            className,
            fileSpec
        )
    }

    private fun TypeSpec.Builder.generateAdapter(
        databaseClassName: ClassName,
        adapter: ColumnAdapterReference
    ): ClassName? = when (adapter.kotlinClassKind) {
        ClassKind.ENUM_ENTRY,
        ClassKind.ENUM_CLASS -> generateEnumAdapter(
            databaseClassName,
            adapter
        )

        ClassKind.INTERFACE,
        ClassKind.CLASS,
        ClassKind.OBJECT,
        ClassKind.ANNOTATION_CLASS,
        null -> null
    }


/*
    public object GenderStringAdapter : ColumnAdapter<UserEntity.Gender, String> {
        override fun decode(databaseValue: String): UserEntity.Gender =
            enumValueOf(databaseValue)

        override fun encode(value: UserEntity.Gender): String =
            value.name
    }
*/

    private fun TypeSpec.Builder.generateEnumAdapter(
        databaseClassName: ClassName,
        adapter: ColumnAdapterReference
    ): ClassName {
        val adapterType = ColumnAdapter::class.asClassName()
            .parameterizedBy(adapter.kotlinType, adapter.affinityType)

        val adapterName = buildString {
            append(adapter.kotlinType.simpleNames.joinToString(""))
            append(adapter.affinityType.simpleName)
            append("Adapter")
        }

        val className = ClassName(
            databaseClassName.packageName,
            databaseClassName.simpleName,
            adapterName
        )

        val decodeParameterName = "databaseValue"
        val encodeParameterName = "value"
        val decodeFunction = FunSpec.builder(ColumnAdapter<Enum<*>, String>::decode.name)
            .addParameter(decodeParameterName, adapter.affinityType)
            .returns(adapter.kotlinType)
            .addModifiers(KModifier.OVERRIDE)
            .addStatement("return enumValueOf($decodeParameterName)")
            .build()

        val encodeFunction = FunSpec.builder(ColumnAdapter<Enum<*>, String>::encode.name)
            .addParameter(encodeParameterName, adapter.kotlinType)
            .returns(adapter.affinityType)
            .addModifiers(KModifier.OVERRIDE)
            .addStatement("return $encodeParameterName.name")
            .build()

        val adapterSpec = TypeSpec.objectBuilder(className)
            .addSuperinterface(adapterType)
            .addFunction(decodeFunction)
            .addFunction(encodeFunction)
            .build()

        addType(adapterSpec)
        return className
    }

    private fun createSchemeObjectSpec(
        className: ClassName,
        databaseSpec: DatabaseSpec,
        generatedTables: Set<TableGenerator.Result>
    ): TypeSpec {
        val classBuilder = TypeSpec.objectBuilder(className)
        val returnType = QueryResult.AsyncValue::class.asTypeName()
            .parameterizedBy(Unit::class.asTypeName())

        val superClassName = SqlSchema::class.asClassName()
            .parameterizedBy(returnType)

        classBuilder.addSuperinterface(superClassName)

        val versionPropertyBuilder = SqlSchema<*>::version.buildSpec()
            .addModifiers(KModifier.OVERRIDE)
            .initializer(databaseSpec.version.toString())

        classBuilder.addProperty(versionPropertyBuilder.build())

        val createFunction = SqlSchema<*>::create.buildSpec()
        val driverName = createFunction.parameters.first().name
        val createFunctionBuilder = createFunction
            .addModifiers(KModifier.OVERRIDE)
            .returns(returnType)

        val createFunctionCodeBuilder = CodeBlock.builder()
            .beginControlFlow("return %T", returnType)

        generatedTables.forEach { generatedTable ->
            createFunctionCodeBuilder.addStatement("%T.create($driverName)", generatedTable.className)
        }

        createFunctionCodeBuilder.endControlFlow()
        createFunctionBuilder.addCode(createFunctionCodeBuilder.build())

        classBuilder.addFunction(createFunctionBuilder.build())

        val migrateFunction = SqlSchema<*>::migrate.buildSpec()
        val migrateFunctionBuilder = migrateFunction
            .addModifiers(KModifier.OVERRIDE)
            .returns(returnType)

        val migrateFunctionCodeBuilder = CodeBlock.builder()
            .beginControlFlow("return %T", returnType)
            .addStatement("// TODO: Not yet implemented in Kabin")
            // TODO: Add migrations when support before close call
            .endControlFlow()

        migrateFunctionBuilder.addCode(migrateFunctionCodeBuilder.build())
        classBuilder.addFunction(migrateFunctionBuilder.build())

        return classBuilder.build()
    }
}
