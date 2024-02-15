package com.attafitamim.kabin.specs.index

import com.attafitamim.kabin.annotations.index.Index

data class IndexSpec(
    val columns: List<String>?,
    val orders: List<Index.Order>?,
    val name: String?,
    val unique: Boolean
)
