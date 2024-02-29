package com.attafitamim.kabin.processor.ksp.options

data class KabinOptions(
    private val options: Map<String, String>
) {

    fun get(
        key: Key,
        orElse: String? = null
    ): String? = options[key.name] ?: orElse

    fun getOrDefault(key: Key): String
        = get(key) ?: key.default

    enum class Key(val default: String) {
        TABLE_SUFFIX("KabinTable"),
        DATABASE_SUFFIX("KabinDatabase"),
        DAO_SUFFIX("KabinDao"),
        DAO_QUERIES_SUFFIX("KabinQueries"),
        INDEX_NAME_PREFIX("index")
    }
}