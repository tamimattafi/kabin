package com.attafitamim.kabin.compiler.sql.utils.sql.column

import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.compiler.sql.syntax.SQLBuilder
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.AUTO_INCREMENT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.DEFAULT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.NOT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.NULL
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.PRIMARY_KEY
import com.attafitamim.kabin.compiler.sql.utils.sql.sqlType
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.column.ColumnTypeSpec

val ColumnSpec.sqlType: ColumnInfo.TypeAffinity
    get() = when (val type = typeAffinity) {
        ColumnInfo.TypeAffinity.INTEGER,
        ColumnInfo.TypeAffinity.NUMERIC,
        ColumnInfo.TypeAffinity.REAL,
        ColumnInfo.TypeAffinity.TEXT,
        ColumnInfo.TypeAffinity.NONE -> type
        ColumnInfo.TypeAffinity.UNDEFINED,
        null -> typeSpec.declaration.sqlType
    }

fun SQLBuilder.appendColumnDefinition(
    columnSpec: ColumnSpec,
    hasSinglePrimaryKey: Boolean,
    isLastStatement: Boolean
) = appendStatement(!isLastStatement) {
    val type = columnSpec.sqlType

    append(columnSpec.name, type.name)

    columnSpec.primaryKeySpec?.let { spec ->
        if (hasSinglePrimaryKey) {
            PRIMARY_KEY
        }

        if (spec.autoGenerate) {
            AUTO_INCREMENT
        }
    }

    if (!columnSpec.typeSpec.isNullable) {
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
    val lastKey = primaryKeys.last()

    PRIMARY_KEY.wrap {
        primaryKeys.forEach { primaryKey ->
            val isLastKey = primaryKey == lastKey
            appendStatement(!isLastKey) {
                append(primaryKey)
            }
        }
    }
}
