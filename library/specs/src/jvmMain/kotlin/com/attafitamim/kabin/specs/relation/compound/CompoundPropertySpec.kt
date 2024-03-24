package com.attafitamim.kabin.specs.relation.compound

import com.attafitamim.kabin.specs.dao.DataTypeSpec
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

data class CompoundPropertySpec(
    val declaration: KSPropertyDeclaration,
    val dataTypeSpec: DataTypeSpec
)
