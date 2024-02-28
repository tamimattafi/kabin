package com.attafitamim.kabin.specs.dao

import com.attafitamim.kabin.annotations.dao.OnConflictStrategy
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.symbol.KSClassDeclaration

sealed interface DaoActionSpec {

    sealed interface EntityAction : DaoActionSpec {
        val entitySpec: EntitySpec?
    }

    sealed interface QueryAction : DaoActionSpec

    data class Upsert(
        override val entitySpec: EntitySpec?
    ) : EntityAction

    data class Update(
        override val entitySpec: EntitySpec?,
        val onConflict: OnConflictStrategy?
    ) : EntityAction

    data class Insert(
        override val entitySpec: EntitySpec?,
        val onConflict: OnConflictStrategy?
    ) : EntityAction

    data class Delete(
        override val entitySpec: EntitySpec?
    ) : EntityAction

    data class RawQuery(
        val observedEntities: List<KSClassDeclaration>?
    ) : QueryAction

    data class Query(
        val value: String
    ) : QueryAction
}
