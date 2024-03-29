package com.attafitamim.kabin.processor.spec

import com.attafitamim.kabin.annotations.Dao
import com.attafitamim.kabin.annotations.Delete
import com.attafitamim.kabin.annotations.Insert
import com.attafitamim.kabin.annotations.OnConflictStrategy
import com.attafitamim.kabin.annotations.Query
import com.attafitamim.kabin.annotations.RawQuery
import com.attafitamim.kabin.annotations.Transaction
import com.attafitamim.kabin.annotations.Update
import com.attafitamim.kabin.annotations.Upsert
import com.attafitamim.kabin.processor.utils.argumentsMap
import com.attafitamim.kabin.processor.utils.getAnnotationArgumentsMap
import com.attafitamim.kabin.processor.utils.getClassDeclaration
import com.attafitamim.kabin.processor.utils.getClassDeclarations
import com.attafitamim.kabin.processor.utils.getEnumArgument
import com.attafitamim.kabin.processor.utils.getReturnTypeSpec
import com.attafitamim.kabin.processor.utils.isInstanceOf
import com.attafitamim.kabin.processor.utils.requireArgument
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.dao.DaoActionSpec
import com.attafitamim.kabin.specs.dao.DaoParameterSpec
import com.attafitamim.kabin.specs.dao.DaoFunctionSpec
import com.attafitamim.kabin.specs.dao.DaoSpec
import com.attafitamim.kabin.specs.dao.TransactionSpec
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSValueParameter

class DaoSpecProcessor(private val logger: KSPLogger) {

    private val daoAnnotation = Dao::class

    private val entitySpecProcessor = EntitySpecProcessor(logger)

    fun getDaoSpec(classDeclaration: KSClassDeclaration): DaoSpec {
        validateClass(classDeclaration)

        val functionSpecs = classDeclaration.getDeclaredFunctions()
            .toList()
            .map(::getFunctionSpec)

        return DaoSpec(
            classDeclaration,
            functionSpecs
        )
    }

    private fun getFunctionSpec(functionDeclaration: KSFunctionDeclaration): DaoFunctionSpec {
        val parameterSpecs = functionDeclaration.parameters.map { valueParameter ->
            getFunctionParameterSpec(valueParameter)
        }

        val transactionArgumentsMap = functionDeclaration
            .getAnnotationArgumentsMap(Transaction::class)
            ?.run {
                TransactionSpec
            }

        val actionTypeSpec = getActionSpec(functionDeclaration)

        val returnTypeSpec = functionDeclaration.returnType?.let { type ->
            entitySpecProcessor.getReturnTypeSpec(type)
        }

        return DaoFunctionSpec(
            functionDeclaration,
            parameterSpecs,
            transactionArgumentsMap,
            actionTypeSpec,
            returnTypeSpec
        )
    }

    private fun getFunctionParameterSpec(
        parameterDeclaration: KSValueParameter
    ): DaoParameterSpec {
        val name = parameterDeclaration.name?.asString()
        if (name.isNullOrBlank()) {
            logger.throwException("Could not determine parameter name", parameterDeclaration)
        }

        val typeSpec = entitySpecProcessor.getReturnTypeSpec(parameterDeclaration.type)
            ?: logger.throwException("Could not determine the type of $name", parameterDeclaration)

        return DaoParameterSpec(
            parameterDeclaration,
            name,
            typeSpec
        )
    }

    private fun getActionSpec(functionDeclaration: KSFunctionDeclaration): DaoActionSpec? {
        functionDeclaration.annotations.forEach { annotation ->
            val argumentsMap = annotation.argumentsMap

            when {
                annotation.isInstanceOf(Delete::class) -> {
                    val entityDeclaration = argumentsMap.getClassDeclaration(Delete::entity.name)
                    val entitySpec = entityDeclaration?.let(entitySpecProcessor::getEntitySpec)

                    return DaoActionSpec.Delete(entitySpec)
                }

                annotation.isInstanceOf(Insert::class) -> {
                    val entityDeclaration = argumentsMap.getClassDeclaration(Insert::entity.name)
                    val entitySpec = entityDeclaration?.let(entitySpecProcessor::getEntitySpec)

                    return DaoActionSpec.Insert(
                        entitySpec,
                        argumentsMap.getEnumArgument<OnConflictStrategy>(Insert::onConflict.name)
                    )
                }

                annotation.isInstanceOf(Upsert::class) -> {
                    val entityDeclaration = argumentsMap.getClassDeclaration(Upsert::entity.name)
                    val entitySpec = entityDeclaration?.let(entitySpecProcessor::getEntitySpec)

                    return DaoActionSpec.Upsert(entitySpec)
                }

                annotation.isInstanceOf(Update::class) -> {
                    val entityDeclaration = argumentsMap.getClassDeclaration(Update::entity.name)
                    val entitySpec = entityDeclaration?.let(entitySpecProcessor::getEntitySpec)

                    return DaoActionSpec.Update(
                        entitySpec,
                        argumentsMap.getEnumArgument<OnConflictStrategy>(Update::onConflict.name)
                    )
                }

                annotation.isInstanceOf(Query::class) -> return DaoActionSpec.Query(
                    argumentsMap.requireArgument(Query::value.name)
                )

                annotation.isInstanceOf(RawQuery::class) -> {
                    val entitiesDeclarations = argumentsMap
                        .getClassDeclarations(RawQuery::observedEntities.name)

                    val entitiesSpecs = entitiesDeclarations?.map(entitySpecProcessor::getEntitySpec)
                    return DaoActionSpec.RawQuery(entitiesSpecs)
                }
            }
        }

        return null
    }


    private fun validateClass(classDeclaration: KSClassDeclaration) {
        if (classDeclaration.classKind != ClassKind.CLASS
            && classDeclaration.classKind != ClassKind.INTERFACE) {
            logger.throwException(
                "Only classes and interfaces can be annotated with @${daoAnnotation.simpleName}",
                classDeclaration
            )
        }

        if (!classDeclaration.isAbstract()) {
            logger.throwException(
                "Dao annotated with @${daoAnnotation.simpleName} must be abstract",
                classDeclaration
            )
        }
    }
}