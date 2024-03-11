package com.attafitamim.kabin.compiler.sql.utils.sql.entity

import com.attafitamim.kabin.compiler.sql.syntax.SQLBuilder
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.AFTER
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.BEFORE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.BEGIN
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.CREATE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.DELETE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.DROP
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.END
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.EXISTS
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.FROM
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.FTS4
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.IF
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.INSERT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.INTO
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.LINE_END
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.NOT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.ON
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.Sign.NAME_SEPARATOR
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.TABLE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.TRIGGER
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.UPDATE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.USING
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.VALUES
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.VIRTUAL
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.WHERE
import com.attafitamim.kabin.compiler.sql.utils.sql.buildSQLQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.column.appendColumnDefinition
import com.attafitamim.kabin.compiler.sql.utils.sql.column.appendPrimaryKeysDefinition
import com.attafitamim.kabin.compiler.sql.utils.sql.dao.parameters
import com.attafitamim.kabin.compiler.sql.utils.sql.dao.variableParameters
import com.attafitamim.kabin.compiler.sql.utils.sql.index.appendForeignKeyDefinition
import com.attafitamim.kabin.compiler.sql.utils.sql.index.getCreationQuery
import com.attafitamim.kabin.processor.ksp.options.KabinOptions
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.column.ColumnTypeSpec
import com.attafitamim.kabin.specs.entity.EntitySearchSpec
import com.attafitamim.kabin.specs.entity.EntitySpec

val EntitySpec.tableCreationQuery: String get() {
    val searchSpec = searchSpec
    return if (searchSpec != null) {
        getFts4CreationQuery(searchSpec)
    } else {
        simpleCreationQuery
    }
}

val EntitySpec.simpleCreationQuery: String get() = buildSQLQuery {
    val foreignKeys = foreignKeys

    val hasSinglePrimaryKey = primaryKeys.size <= 1
    val hasForeignKeys = !foreignKeys.isNullOrEmpty()

    CREATE; TABLE; IF; NOT; EXISTS(tableName).wrap {
        appendColumnsDefinition(
            columns,
            hasSinglePrimaryKey,
            !hasForeignKeys
        )

        if (!hasSinglePrimaryKey) {
            appendPrimaryKeysDefinition(
                primaryKeys,
                !hasForeignKeys
            )
        }

        foreignKeys?.forEachIndexed { index, foreignKeySpec ->
            val isLastStatement = index == foreignKeys.lastIndex
            appendForeignKeyDefinition(foreignKeySpec, isLastStatement)
        }
    }
}

fun EntitySpec.getFts4CreationQuery(searchSpec: EntitySearchSpec): String = buildSQLQuery {
    val foreignKeys = foreignKeys

    val hasSinglePrimaryKey = primaryKeys.size <= 1
    CREATE; VIRTUAL; TABLE; IF; NOT; EXISTS(tableName); USING; FTS4.wrap {
        appendColumnsDefinition(
            columns,
            hasSinglePrimaryKey,
            isLastColumnsAppend = false
        )

        if (!hasSinglePrimaryKey) {
            appendPrimaryKeysDefinition(
                primaryKeys,
                isLastStatement = false
            )
        }

        foreignKeys?.forEach { foreignKeySpec ->
            appendForeignKeyDefinition(
                foreignKeySpec,
                isLastStatement = false
            )
        }

        appendStatement(includeStatementSeparator = false) {
            append("content=${searchSpec.contentEntity.tableName}")
        }
    }
}

fun SQLBuilder.appendColumnsDefinition(
    columns: List<ColumnSpec>,
    hasSinglePrimaryKey: Boolean,
    isLastColumnsAppend: Boolean
) {
    val flatColumns = getFlatColumns(columns)
    flatColumns.forEachIndexed { index, columnSpec ->
        val isLastStatement = hasSinglePrimaryKey
                && index == flatColumns.lastIndex
                && isLastColumnsAppend

        when (columnSpec.typeSpec.dataType) {
            is ColumnTypeSpec.DataType.Class -> {
                appendColumnDefinition(
                    columnSpec,
                    hasSinglePrimaryKey,
                    isLastStatement
                )
            }

            is ColumnTypeSpec.DataType.Embedded -> {
                return@forEachIndexed
            }
        }
    }
}

val EntitySpec.tableDropQuery: String get() = buildSQLQuery {
    DROP; TABLE; IF; EXISTS(tableName)
}

val EntitySpec.tableClearQuery: String get() = buildSQLQuery {
    DELETE; FROM(tableName)
}

fun EntitySpec.getIndicesCreationQueries(options: KabinOptions): List<String>? {
    val prefix = options.getOrDefault(KabinOptions.Key.INDEX_NAME_PREFIX)
    return indices?.map { indexSpec ->
        indexSpec.getCreationQuery(tableName, prefix)
    }
}

fun EntitySpec.getTriggersCreationQueries(options: KabinOptions): List<String>? {
    val searchSpec = searchSpec ?: return null
    return listOf(
        getFtsBeforeUpdateTrigger(options, searchSpec),
        getFtsBeforeDeleteTrigger(options, searchSpec),
        getFtsAfterUpdateTrigger(options, searchSpec),
        getFtsAfterInsertTrigger(options, searchSpec)
    )
}

fun getFlatColumns(columns: List<ColumnSpec>): List<ColumnSpec> {
    val parameters = ArrayList<ColumnSpec>()
    columns.forEach { columnSpec ->
        when (val type = columnSpec.typeSpec.dataType) {
            is ColumnTypeSpec.DataType.Class -> {
                parameters.add(columnSpec)
            }

            is ColumnTypeSpec.DataType.Embedded -> {
                parameters.addAll(getFlatColumns(type.columns))
            }
        }
    }

    return parameters
}

fun EntitySpec.getFtsBeforeUpdateTrigger(
    options: KabinOptions,
    searchSpec: EntitySearchSpec
): String = buildSQLQuery {
    val triggerName = getFtsTriggerName(
        options,
        KabinOptions.Key.BEFORE_UPDATE_TRIGGER_NAME_SUFFIX
    )

    CREATE; TRIGGER; IF; NOT; EXISTS(triggerName); BEFORE; UPDATE
    ON(searchSpec.contentEntity.tableName); BEGIN; DELETE; FROM(tableName)
    WHERE("docid=OLD.rowid"); LINE_END; END
}

fun EntitySpec.getFtsBeforeDeleteTrigger(
    options: KabinOptions,
    searchSpec: EntitySearchSpec
): String = buildSQLQuery {
    val triggerName = getFtsTriggerName(
        options,
        KabinOptions.Key.BEFORE_DELETE_TRIGGER_NAME_SUFFIX
    )

    CREATE; TRIGGER; IF; NOT; EXISTS(triggerName); BEFORE; DELETE
    ON(searchSpec.contentEntity.tableName); BEGIN; DELETE; FROM(tableName)
    WHERE("docid=OLD.rowid"); LINE_END; END
}

fun EntitySpec.getFtsAfterUpdateTrigger(
    options: KabinOptions,
    searchSpec: EntitySearchSpec
): String = buildSQLQuery {
    val triggerName = getFtsTriggerName(
        options,
        KabinOptions.Key.AFTER_UPDATE_TRIGGER_NAME_SUFFIX
    )

    val columns = getFlatColumns(columns).map(ColumnSpec::name)
    val insertParameters = listOf("docid") + columns
    val valueParameters = (listOf("rowid") + columns).map { column ->
        "NEW.$column"
    }

    CREATE; TRIGGER; IF; NOT; EXISTS(triggerName); AFTER; UPDATE
    ON(searchSpec.contentEntity.tableName);
    BEGIN; INSERT; INTO(tableName).parameters(insertParameters)
    VALUES.parameters(valueParameters); LINE_END; END
}

fun EntitySpec.getFtsAfterInsertTrigger(
    options: KabinOptions,
    searchSpec: EntitySearchSpec
): String = buildSQLQuery {
    val triggerName = getFtsTriggerName(
        options,
        KabinOptions.Key.AFTER_INSERT_TRIGGER_NAME_SUFFIX
    )

    val columns = getFlatColumns(columns).map(ColumnSpec::name)
    val insertParameters = listOf("docid") + columns
    val valueParameters = (listOf("rowid") + columns).map { column ->
        "NEW.$column"
    }

    CREATE; TRIGGER; IF; NOT; EXISTS(triggerName); AFTER; INSERT
    ON(searchSpec.contentEntity.tableName); BEGIN; INSERT
    INTO(tableName).parameters(insertParameters); VALUES.parameters(valueParameters)
    LINE_END; END
}

fun EntitySpec.getFtsTriggerName(
    options: KabinOptions,
    suffixKey: KabinOptions.Key
): String {
    val prefix = options.getOrDefault(KabinOptions.Key.FTS_TRIGGER_NAME_PREFIX)
    val suffix = options.getOrDefault(suffixKey)

    return buildString {
        append(
            prefix,
            NAME_SEPARATOR,
            tableName,
            NAME_SEPARATOR,
            suffix
        )
    }
}