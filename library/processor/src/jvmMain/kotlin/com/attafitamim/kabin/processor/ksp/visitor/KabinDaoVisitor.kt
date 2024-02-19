package com.attafitamim.kabin.processor.ksp.visitor

import com.attafitamim.kabin.annotations.dao.Dao
import com.attafitamim.kabin.annotations.dao.Delete
import com.attafitamim.kabin.annotations.dao.Insert
import com.attafitamim.kabin.annotations.dao.OnConflictStrategy
import com.attafitamim.kabin.annotations.dao.Query
import com.attafitamim.kabin.annotations.dao.RawQuery
import com.attafitamim.kabin.annotations.dao.Transaction
import com.attafitamim.kabin.annotations.dao.Update
import com.attafitamim.kabin.annotations.dao.Upsert
import com.attafitamim.kabin.annotations.database.Database
import com.attafitamim.kabin.processor.handler.KabinSpecHandler
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.utils.argumentsMap
import com.attafitamim.kabin.processor.utils.getAnnotationArgumentsMap
import com.attafitamim.kabin.processor.utils.getArgument
import com.attafitamim.kabin.processor.utils.isInstanceOf
import com.attafitamim.kabin.processor.utils.requireArgument
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.dao.DaoActionSpec
import com.attafitamim.kabin.specs.dao.DaoFunctionSpec
import com.attafitamim.kabin.specs.dao.DaoSpec
import com.attafitamim.kabin.specs.dao.TransactionSpec
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid

class KabinDaoVisitor(
    private val specHandler: KabinSpecHandler,
    private val logger: KSPLogger,
    private val options: KabinOptions
) : KSVisitorVoid() {

    private val daoAnnotation = Dao::class

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        validateClass(classDeclaration)
        val daoSpec = getDaoSpec(classDeclaration)
        specHandler.handleDaoSpec(daoSpec)
    }

    private fun getDaoSpec(classDeclaration: KSClassDeclaration): DaoSpec {
        val functionSpecs = classDeclaration.getDeclaredFunctions()
            .toList()
            .map(::getFunctionSpec)

        return DaoSpec(
            classDeclaration,
            functionSpecs
        )
    }

    private fun getFunctionSpec(functionDeclaration: KSFunctionDeclaration): DaoFunctionSpec {
        val transactionArgumentsMap = functionDeclaration
            .getAnnotationArgumentsMap(Transaction::class)
            ?.run {
                TransactionSpec
            }

        val actionTypeSpec = getActionSpec(functionDeclaration)

        return DaoFunctionSpec(
            functionDeclaration,
            transactionArgumentsMap,
            actionTypeSpec
        )
    }

    private fun getActionSpec(functionDeclaration: KSFunctionDeclaration): DaoActionSpec? {
        functionDeclaration.annotations.forEach { annotation ->
            val argumentsMap = annotation.argumentsMap

            when {
                annotation.isInstanceOf(Delete::class) -> return DaoActionSpec.Delete(
                    argumentsMap.getArgument(Delete::entity.name)
                )

                annotation.isInstanceOf(Insert::class) -> return DaoActionSpec.Insert(
                    argumentsMap.getArgument(Insert::entity.name),
                    argumentsMap.getArgument(Insert::onConflict.name)
                )

                annotation.isInstanceOf(Upsert::class) -> return DaoActionSpec.Upsert(
                    argumentsMap.getArgument(Upsert::entity.name),
                )

                annotation.isInstanceOf(Update::class) -> return DaoActionSpec.Update(
                    argumentsMap.getArgument(Update::entity.name),
                    argumentsMap.getArgument(Update::onConflict.name)
                )

                annotation.isInstanceOf(Query::class) -> return DaoActionSpec.Query(
                    argumentsMap.requireArgument(Query::value.name)
                )

                annotation.isInstanceOf(RawQuery::class) -> return DaoActionSpec.RawQuery(
                    argumentsMap.getArgument(RawQuery::observedEntities.name)
                )
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