package com.attafitamim.kabin.specs.dao

import com.attafitamim.kabin.annotations.OnConflictStrategy
import com.attafitamim.kabin.specs.entity.EntitySpec

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
        val observedEntities: List<EntitySpec>?
    ) : QueryAction

    data class Query(
        val value: String
    ) : QueryAction
}
