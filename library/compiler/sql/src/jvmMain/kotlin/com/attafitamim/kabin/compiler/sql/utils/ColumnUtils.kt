package com.attafitamim.kabin.compiler.sql.utils

import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.compiler.sql.syntax.SQLBuilder
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.AUTO_INCREMENT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.DEFAULT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.NOT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.NULL
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.PRIMARY_KEY
import com.attafitamim.kabin.specs.column.ColumnSpec

val ColumnSpec.actualName: String get() = name ?: declaration.simpleName.asString()

val ColumnSpec.sqlType: ColumnInfo.TypeAffinity
    get() = when (val type = typeAffinity) {
        ColumnInfo.TypeAffinity.INTEGER,
        ColumnInfo.TypeAffinity.NUMERIC,
        ColumnInfo.TypeAffinity.REAL,
        ColumnInfo.TypeAffinity.TEXT,
        ColumnInfo.TypeAffinity.NONE -> type
        ColumnInfo.TypeAffinity.UNDEFINED,
        null -> declaration.sqlType
    }

fun SQLBuilder.appendColumnDefinition(
    columnSpec: ColumnSpec,
    hasSinglePrimaryKey: Boolean,
    isLastStatement: Boolean
) = appendStatement(!isLastStatement) {
    val type = columnSpec.sqlType
    val isNullable = columnSpec.declaration.type.resolve().isMarkedNullable

    append(columnSpec.actualName, type.name)

    columnSpec.primaryKeySpec?.let { spec ->
        if (hasSinglePrimaryKey) {
            PRIMARY_KEY
        }

        if (spec.autoGenerate) {
            AUTO_INCREMENT
        }
    }

    if (!isNullable) {
        NOT; NULL
    }

    val defaultValue = columnSpec.defaultValue
    if (!defaultValue.isNullOrBlank()) {
        DEFAULT(defaultValue)
    }
}

fun SQLBuilder.appendPrimaryKeysDefinition(
    primaryKeys: Set<String>
) = appendStatement {
    PRIMARY_KEY

    val lastKey = primaryKeys.last()
    wrap {
        primaryKeys.forEach { primaryKey ->
            val isLastKey = primaryKey == lastKey
            appendStatement(!isLastKey) {
                append(primaryKey)
            }
        }
    }
}
