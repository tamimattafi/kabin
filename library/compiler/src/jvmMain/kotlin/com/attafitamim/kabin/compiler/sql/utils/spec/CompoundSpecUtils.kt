package com.attafitamim.kabin.compiler.sql.utils.spec

import com.attafitamim.kabin.specs.dao.DataTypeSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.attafitamim.kabin.specs.relation.compound.CompoundSpec

fun CompoundSpec.getQueriedKeys(): Set<String> {
    val queriedKeys = LinkedHashSet<String>()
    val mainPropertyKeys = mainProperty.dataTypeSpec.getQueriedKeys()
    queriedKeys.addAll(mainPropertyKeys)

    relations.forEach { relationSpec ->
        val relationPropertyKeys = relationSpec.property.dataTypeSpec.getQueriedKeys()
        queriedKeys.addAll(relationPropertyKeys)

        val relationEntityKeys = relationSpec.relation.entitySpec?.getQueriedKeys().orEmpty()
        queriedKeys.addAll(relationEntityKeys)

        val relationJunctionKeys = relationSpec.relation.junctionSpec?.entitySpec?.getQueriedKeys().orEmpty()
        queriedKeys.addAll(relationJunctionKeys)
    }

    return queriedKeys
}

fun DataTypeSpec.getQueriedKeys(): Set<String> =
    when (val type = dataType) {
        is DataTypeSpec.DataType.Class -> emptySet()
        is DataTypeSpec.DataType.Compound -> type.compoundSpec.getQueriedKeys()
        is DataTypeSpec.DataType.Entity -> type.entitySpec.getQueriedKeys()
        is DataTypeSpec.DataType.Wrapper -> {
            val dataType = type.nestedTypeSpec.getNestedDataType()
            dataType.getQueriedKeys()
        }
    }

fun EntitySpec.getQueriedKeys(): Set<String> = setOf(tableName)