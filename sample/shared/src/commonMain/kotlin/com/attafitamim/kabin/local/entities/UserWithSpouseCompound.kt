package com.attafitamim.kabin.local.entities

import com.attafitamim.kabin.annotations.entity.Embedded
import com.attafitamim.kabin.annotations.relation.Relation

data class UserWithSpouseCompound(
    @Embedded
    val userEntity: UserEntity,

    @Relation(
        entity = UserEntity::class,
        parentColumn = "spouseId",
        entityColumn = "id"
    )
    val spouse: UserEntity?,

    @Relation(
        entity = UserEntity::class,
        parentColumn = "spouseId",
        entityColumn = "id"
    )
    val spouses: List<UserEntity>?
)
