package com.attafitamim.kabin.specs.database

import com.attafitamim.kabin.specs.dao.DaoSpec
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

data class DatabaseDaoGetterSpec(
    val declaration: KSFunctionDeclaration,
    val daoSpec: DaoSpec
)
