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
        ENTITY_MAPPER_SUFFIX("KabinMapper"),
        DATABASE_SUFFIX("KabinDatabase"),
        DAO_SUFFIX("KabinDao"),
        DAO_QUERIES_SUFFIX("KabinQueries"),
        INDEX_NAME_PREFIX("index"),
        FTS_TRIGGER_NAME_PREFIX("kabin_fts_content_sync"),
        BEFORE_UPDATE_TRIGGER_NAME_SUFFIX("BEFORE_UPDATE"),
        AFTER_UPDATE_TRIGGER_NAME_SUFFIX("AFTER_UPDATE"),
        BEFORE_DELETE_TRIGGER_NAME_SUFFIX("BEFORE_DELETE"),
        AFTER_INSERT_TRIGGER_NAME_SUFFIX("AFTER_INSERT")
    }
}