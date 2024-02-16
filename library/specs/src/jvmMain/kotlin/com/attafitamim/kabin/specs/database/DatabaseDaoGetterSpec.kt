package com.attafitamim.kabin.specs.database

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

data class DatabaseDaoGetterSpec(
    val declaration: KSFunctionDeclaration,
    val daoDeclaration: KSClassDeclaration
)
