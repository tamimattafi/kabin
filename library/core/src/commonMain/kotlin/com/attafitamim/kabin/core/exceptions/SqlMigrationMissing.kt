package com.attafitamim.kabin.core.exceptions

class SqlMigrationMissing(
    private val oldVersion: Long,
    private val newVersion: Long
) : Exception() {

    override val message: String
        get() = "Missing migration from $oldVersion to $newVersion"
}