package com.attafitamim.kabin.annotations.column

@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ColumnInfo(
    val name: String = DEFAULT_COLUMN_NAME,
    val typeAffinity: TypeAffinity = TypeAffinity.UNDEFINED,
    val index: Boolean = DEFAULT_INDEX,
    val collate: Collate = Collate.UNSPECIFIED,
    val defaultValue: String = DEFAULT_VALUE,
) {

    companion object {
        const val DEFAULT_COLUMN_NAME: String = ""
        const val DEFAULT_VALUE: String = ""
        const val DEFAULT_INDEX = false
    }

    enum class TypeAffinity {
        UNDEFINED,
        INTEGER,
        NUMERIC,
        REAL,
        TEXT,
        NONE
    }

    enum class Collate {
        UNSPECIFIED,
        BINARY,
        NOCASE,
        RTRIM,
        LOCALIZED,
        UNICODE
    }
}
