package com.attafitamim.kabin.annotations

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Entity(
    val tableName: String = DEFAULT_TABLE_NAME,
    val indices: Array<Index> = [],
    val inheritSuperIndices: Boolean = DEFAULT_INHERIT_SUPER_INDICES,
    val primaryKeys: Array<String> = [],
    val foreignKeys: Array<ForeignKey> = [],
    val ignoredColumns: Array<String> = []
) {

    companion object {
        const val DEFAULT_TABLE_NAME = ""
        const val DEFAULT_INHERIT_SUPER_INDICES = false
    }
}
