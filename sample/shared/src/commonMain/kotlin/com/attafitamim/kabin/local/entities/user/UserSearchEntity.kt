package com.attafitamim.kabin.local.entities.user

import com.attafitamim.kabin.annotations.Entity
import com.attafitamim.kabin.annotations.Fts4

@Fts4(contentEntity = UserEntity::class)
@Entity
data class UserSearchEntity(
    val phoneNumber: String,
    val name: String
)
