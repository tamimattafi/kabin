package com.attafitamim.kabin.specs.relation.compound

import com.google.devtools.ksp.symbol.KSClassDeclaration

data class CompoundSpec(
    val declaration: KSClassDeclaration,
    val mainProperty: CompoundPropertySpec,
    val relations: List<CompoundRelationSpec>
)
