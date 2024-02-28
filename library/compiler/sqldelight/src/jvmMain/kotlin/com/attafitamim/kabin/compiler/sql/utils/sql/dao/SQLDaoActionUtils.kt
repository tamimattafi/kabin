package com.attafitamim.kabin.compiler.sql.utils.sql.dao

import com.attafitamim.kabin.annotations.dao.OnConflictStrategy
import com.attafitamim.kabin.compiler.sql.syntax.SQLBuilder
import com.attafitamim.kabin.compiler.sql.syntax.SQLQuery
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.ABORT
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
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.SET
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.UPDATE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.VALUE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.VALUES
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.WHERE
import com.attafitamim.kabin.compiler.sql.utils.sql.buildSQLQuery
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.dao.DaoActionSpec
import com.attafitamim.kabin.specs.entity.EntitySpec

fun DaoActionSpec.EntityAction.getSQLQuery(
    parameterEntitySpec: EntitySpec?
): SQLQuery {
    val actualEntitySpec = requireNotNull(entitySpec ?: parameterEntitySpec)
    return when (this) {
        is DaoActionSpec.Delete -> getSQLQuery(actualEntitySpec)
        is DaoActionSpec.Insert -> getSQLQuery(actualEntitySpec)
        is DaoActionSpec.Update -> getSQLQuery(actualEntitySpec)
        is DaoActionSpec.Upsert -> getSQLQuery(actualEntitySpec)
    }
}

fun DaoActionSpec.Delete.getSQLQuery(
    actualEntitySpec: EntitySpec
): SQLQuery {
    val parameters = actualEntitySpec.primaryKeys
    val query = buildSQLQuery {
        DELETE; FROM(actualEntitySpec.tableName); WHERE.equalParameters(parameters)
    }

    return SQLQuery(query, parameters)
}

fun DaoActionSpec.Insert.getSQLQuery(
    actualEntitySpec: EntitySpec
): SQLQuery {
    val parameters = actualEntitySpec.columns.map(ColumnSpec::name)
    val query = buildSQLQuery {
        INSERT.or(onConflict); INTO(actualEntitySpec.tableName); VALUES.parameters(parameters)
    }

    return SQLQuery(query, parameters)
}

fun DaoActionSpec.Update.getSQLQuery(
    actualEntitySpec: EntitySpec
): SQLQuery {
    val parameters = actualEntitySpec.columns.map(ColumnSpec::name)
    val query = buildSQLQuery {
        UPDATE.or(onConflict)(actualEntitySpec.tableName); SET.namedParameters(parameters)
    }

    return SQLQuery(query, parameters)
}

fun DaoActionSpec.Upsert.getSQLQuery(
    actualEntitySpec: EntitySpec
): SQLQuery {
    val parameters = actualEntitySpec.columns.map(ColumnSpec::name)
    val query = buildSQLQuery {
        INSERT; INTO(actualEntitySpec.tableName); VALUES.parameters(parameters)
    }

    return SQLQuery(query, parameters)
}

private fun SQLBuilder.equalParameters(parameters: Set<String>) {
    wrap {
        parameters.forEach { key ->
            append(key); EQUALS; VALUE

            if (parameters.last() != key) {
                AND
            }
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