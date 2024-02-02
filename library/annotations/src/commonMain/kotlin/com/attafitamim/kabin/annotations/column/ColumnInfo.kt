package com.attafitamim.kabin.annotations.column

@Target(AnnotationTarget.FIELD, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class ColumnInfo(
    val name: String = INHERIT_FIELD_NAME,
    val typeAffinity: TypeAffinity = TypeAffinity.UNDEFINED,
    val index: Boolean = false,
    val collate: Collate = Collate.UNSPECIFIED,
    val defaultValue: String = VALUE_UNSPECIFIED,
) {

    companion object {
        const val INHERIT_FIELD_NAME: String = "[field-name]"
        const val BINARY: Int = 2
        const val VALUE_UNSPECIFIED: String = "[value-unspecified]"
    }

    enum class TypeAffinity {
        UNDEFINED,
        TEXT,
        INTEGER,
        REAL,
        BLOB
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
