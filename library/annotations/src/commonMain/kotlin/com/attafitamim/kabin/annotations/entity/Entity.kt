package com.attafitamim.kabin.annotations.entity

import com.attafitamim.kabin.annotations.column.ForeignKey
import com.attafitamim.kabin.annotations.column.Index

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Entity(
    val tableName: String = "",
    val indices: Array<Index> = [],
    val inheritSuperIndices: Boolean = false,
    val primaryKeys: Array<String> = [],
    val foreignKeys: Array<ForeignKey> = [],
    val ignoredColumns: Array<String> = []
)
