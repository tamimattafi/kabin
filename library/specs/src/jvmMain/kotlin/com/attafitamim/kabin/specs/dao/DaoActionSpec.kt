package com.attafitamim.kabin.specs.dao

import com.attafitamim.kabin.annotations.dao.OnConflictStrategy
import com.google.devtools.ksp.symbol.KSClassDeclaration

sealed interface DaoActionSpec {

    data class Upsert(
        val entityDeclaration: KSClassDeclaration?
    ) : DaoActionSpec

    data class Update(
        val entityDeclaration: KSClassDeclaration?,
        val onConflict: OnConflictStrategy?
    ) : DaoActionSpec

    data class RawQuery(
        val observedEntities: List<KSClassDeclaration>?
    ) : DaoActionSpec

    data class Query(
        val value: String
    ) : DaoActionSpec

    data class Insert(
        val entityDeclaration: KSClassDeclaration?,
        val onConflict: OnConflictStrategy?
    ) : DaoActionSpec

    data class Delete(
        val entityDeclaration: KSClassDeclaration?
    ) : DaoActionSpec
}
