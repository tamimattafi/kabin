package com.attafitamim.kabin.compiler.sql.utils.sql.dao

import com.attafitamim.kabin.annotations.dao.OnConflictStrategy
import com.attafitamim.kabin.compiler.sql.syntax.SQLBuilder
import com.attafitamim.kabin.compiler.sql.syntax.SQLDaoQuery
import com.attafitamim.kabin.compiler.sql.syntax.SQLEntityQuery
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.ABORT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.ALL
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.AND
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.DELETE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.EQUALS
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.FAIL
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.FROM
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.IGNORE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.INSERT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.INTO
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.OR
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.REPLACE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.ROLLBACK
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.SELECT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.SET
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.UPDATE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.VALUE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.VALUES
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.WHERE
import com.attafitamim.kabin.compiler.sql.utils.sql.buildSQLQuery
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.column.ColumnTypeSpec
import com.attafitamim.kabin.specs.dao.DaoActionSpec
import com.attafitamim.kabin.specs.dao.DaoParameterSpec
import com.attafitamim.kabin.specs.dao.DataTypeSpec
import com.attafitamim.kabin.specs.entity.EntitySpec


fun DaoActionSpec.EntityAction.getSQLQuery(
    parameterEntitySpec: EntitySpec?
): SQLEntityQuery {
    val actualEntitySpec = requireNotNull(entitySpec ?: parameterEntitySpec)
    return when (this) {
        is DaoActionSpec.Delete -> getSQLQuery(actualEntitySpec)
        is DaoActionSpec.Insert -> getSQLQuery(actualEntitySpec)
        is DaoActionSpec.Update -> getSQLQuery(actualEntitySpec)
        is DaoActionSpec.Upsert -> getSQLQuery(actualEntitySpec)
    }
}

fun getSelectSQLQuery(
    entitySpec: EntitySpec,
    column: ColumnSpec
): SQLEntityQuery {
    val columns = setOf(column)
    val parameters = setOf(column.name)
    val query = buildSQLQuery {
        SELECT; ALL; FROM(entitySpec.tableName); WHERE.equalParameters(parameters)
    }

    return SQLEntityQuery(query, columns, parameters.size)
}

fun DaoActionSpec.Delete.getSQLQuery(
    actualEntitySpec: EntitySpec
): SQLEntityQuery {
    val parameters = actualEntitySpec.primaryKeys
    val query = buildSQLQuery {
        DELETE; FROM(actualEntitySpec.tableName); WHERE.equalParameters(parameters)
    }

    val columns = actualEntitySpec.columns.filter { columnSpec ->
        parameters.contains(columnSpec.name)
    }

    return SQLEntityQuery(query, columns, parameters.size)
}

fun DaoActionSpec.Insert.getSQLQuery(
    actualEntitySpec: EntitySpec
): SQLEntityQuery {
    val parameters = getFlatColumns(actualEntitySpec.columns)
        .map(ColumnSpec::name)

    val query = buildSQLQuery {
        INSERT.or(onConflict); INTO(actualEntitySpec.tableName); VALUES.parameters(parameters)
    }

    return SQLEntityQuery(query, actualEntitySpec.columns, parameters.size)
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

fun DaoActionSpec.Update.getSQLQuery(
    actualEntitySpec: EntitySpec
): SQLEntityQuery {
    val parameters = getFlatColumns(actualEntitySpec.columns)
        .map(ColumnSpec::name)

    val primaryKeys = actualEntitySpec.primaryKeys
    val query = buildSQLQuery {
        UPDATE.or(onConflict)(actualEntitySpec.tableName); SET.namedParameters(parameters)
        WHERE.equalParameters(actualEntitySpec.primaryKeys)
    }

    val parametersSize = parameters.size + primaryKeys.size
    return SQLEntityQuery(query, actualEntitySpec.columns, parametersSize)
}

fun DaoActionSpec.Upsert.getSQLQuery(
    actualEntitySpec: EntitySpec
): SQLEntityQuery {
    val parameters = getFlatColumns(actualEntitySpec.columns)
        .map(ColumnSpec::name)

    val query = buildSQLQuery {
        INSERT; OR; REPLACE; INTO(actualEntitySpec.tableName); VALUES.parameters(parameters)
    }

    return SQLEntityQuery(query, actualEntitySpec.columns, parameters.size)
}

private fun SQLBuilder.equalParameters(parameters: Set<String>) {
    parameters.forEach { key ->
        append(key); EQUALS; VALUE

        if (parameters.last() != key) {
            AND
        }
    }
}

private fun SQLBuilder.namedParameters(parameters: List<String>) {
    parameters.forEachIndexed { index, column ->
        val isLastStatement = index == parameters.lastIndex
        appendStatement(!isLastStatement) {
            append(column); EQUALS; VALUE
        }
    }
}

private fun SQLBuilder.parameters(parameters: List<String>) {
    wrap {
        parameters.forEachIndexed { index, _ ->
            val isLastStatement = index == parameters.lastIndex
            appendStatement(!isLastStatement) {
                VALUE
            }
        }
    }
}

private fun SQLBuilder.or(onConflictStrategy: OnConflictStrategy?): SQLBuilder =
    if (onConflictStrategy == null || onConflictStrategy == OnConflictStrategy.NONE) {
        this
    } else {
        OR; when (onConflictStrategy) {
            OnConflictStrategy.REPLACE -> REPLACE
            OnConflictStrategy.ROLLBACK -> ROLLBACK
            OnConflictStrategy.ABORT -> ABORT
            OnConflictStrategy.FAIL -> FAIL
            OnConflictStrategy.IGNORE -> IGNORE
            else -> error("This statement should not be reached")
        }
    }