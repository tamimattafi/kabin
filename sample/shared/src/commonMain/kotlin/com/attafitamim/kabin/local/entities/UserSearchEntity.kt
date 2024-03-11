package com.attafitamim.kabin.local.entities

import com.attafitamim.kabin.annotations.entity.Entity
import com.attafitamim.kabin.annotations.entity.Fts4

@Fts4(contentEntity = UserEntity::class)
@Entity
data class UserSearchEntity(
    val phoneNumber: String,
    val name: String
)
