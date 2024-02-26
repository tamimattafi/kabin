package com.attafitamim.kabin.core.table

interface KabinTable {
    val creationQuery: String
    val dropQuery: String
    val clearQuery: String
    val indicesCreationQueries: List<String>?
}
