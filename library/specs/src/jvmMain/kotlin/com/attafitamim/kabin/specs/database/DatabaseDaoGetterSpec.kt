package com.attafitamim.kabin.specs.database

import com.attafitamim.kabin.specs.dao.DaoSpec
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

data class DatabaseDaoGetterSpec(
    val declaration: KSPropertyDeclaration,
    val daoSpec: DaoSpec
)
