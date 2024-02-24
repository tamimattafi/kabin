package com.attafitamim.kabin.compiler.sql.utils

import com.attafitamim.kabin.annotations.column.ColumnInfo
import com.attafitamim.kabin.compiler.sql.syntax.SQLBuilder
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.Operator.AUTO_INCREMENT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.Operator.CREATE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.Operator.DEFAULT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.Operator.EXITS
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.Operator.IF
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.Operator.NOT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.Type.PRIMARY_KEY
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.Type.TABLE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.Value.NULL
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

val typesMap = mapOf(
    Int::class.qualifiedName to ColumnInfo.TypeAffinity.INTEGER,
    Long::class.qualifiedName to ColumnInfo.TypeAffinity.INTEGER,
    Float::class.qualifiedName to ColumnInfo.TypeAffinity.REAL,
    Double::class.qualifiedName to ColumnInfo.TypeAffinity.REAL,
    String::class.qualifiedName to ColumnInfo.TypeAffinity.TEXT
)

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

val KSPropertyDeclaration.sqlType: ColumnInfo.TypeAffinity
    get() {
        val qualifiedName = type.resolve().declaration.run {
            qualifiedName ?: simpleName
        }.asString()

        return typesMap.getValue(qualifiedName)
    }

val EntitySpec.sqlCreationQuery: String get() = buildSQLQuery {
    CREATE; TABLE; IF; NOT; EXITS(actualTableName)

    val primaryKeys = LinkedHashSet(primaryKeys.orEmpty())
    val ignoredColumns = LinkedHashSet(ignoredColumns.orEmpty())

    val actualColumns = columns.filter { columnSpec ->
        if (columnSpec.primaryKeySpec != null) {
            primaryKeys.add(columnSpec.actualName)
        }

        columnSpec.ignoreSpec == null && !ignoredColumns.contains(columnSpec.actualName)
    }

    val hasSinglePrimaryKey = primaryKeys.size <= 1
    wrap {
        actualColumns.forEachIndexed { index, columnSpec ->
            val isLastStatement = hasSinglePrimaryKey && index == actualColumns.lastIndex
            appendColumnDefinition(
                columnSpec,
                hasSinglePrimaryKey,
                isLastStatement
            )
        }

        if (!hasSinglePrimaryKey) {
            appendPrimaryKeysDefinition(primaryKeys)
        }
    }
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

fun buildSQLQuery(
    includeStatementSeparator: Boolean = false,
    builder: SQLBuilder.() -> Unit
): String = SQLBuilder().appendStatement(
    includeStatementSeparator,
    builder
).raw
