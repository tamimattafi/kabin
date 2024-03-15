package com.attafitamim.kabin.core.database

interface KabinDatabase {
    suspend fun dropTables()
    suspend fun clearTables()
}
