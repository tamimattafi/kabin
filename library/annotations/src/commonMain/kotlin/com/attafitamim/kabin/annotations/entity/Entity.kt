package com.attafitamim.kabin.annotations.entity

import com.attafitamim.kabin.annotations.relation.ForeignKey
import com.attafitamim.kabin.annotations.index.Index

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
