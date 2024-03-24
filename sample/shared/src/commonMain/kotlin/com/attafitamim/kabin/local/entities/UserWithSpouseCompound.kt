package com.attafitamim.kabin.local.entities

import com.attafitamim.kabin.annotations.Embedded
import com.attafitamim.kabin.annotations.Relation

data class UserWithSpouseCompound(
    @Embedded
    val mainEntity: UserEntity,

    @Relation(
        entity = UserEntity::class,
        parentColumn = "spouseId",
        entityColumn = "id"
    )
    val relationEntity1: UserEntity?,

    @Relation(
        entity = UserEntity::class,
        parentColumn = "id",
        entityColumn = "spouseId"
    )
    val relationEntity2List: List<UserEntity>
)
