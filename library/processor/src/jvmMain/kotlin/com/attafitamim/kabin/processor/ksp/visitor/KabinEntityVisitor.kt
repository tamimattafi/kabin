package com.attafitamim.kabin.processor.ksp.visitor

import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.annotations.column.Ignore
import com.attafitamim.kabin.annotations.entity.Entity
import com.attafitamim.kabin.annotations.relation.ForeignKey
import com.attafitamim.kabin.annotations.index.Index
import com.attafitamim.kabin.annotations.index.PrimaryKey
import com.attafitamim.kabin.processor.handler.KabinSpecHandler
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.processor.spec.EntitySpecProcessor
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
    logger: KSPLogger,
    private val options: KabinOptions
): KSVisitorVoid() {

    private val entitySpecProcessor = EntitySpecProcessor(logger)

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        val entitySpec = entitySpecProcessor.getEntitySpec(classDeclaration)
        specHandler.handleEntitySpec(entitySpec)
    }
}
