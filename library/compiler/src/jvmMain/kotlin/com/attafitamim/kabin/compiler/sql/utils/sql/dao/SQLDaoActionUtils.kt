package com.attafitamim.kabin.compiler.sql.utils.sql.dao

import com.attafitamim.kabin.annotations.dao.OnConflictStrategy
import com.attafitamim.kabin.compiler.sql.generator.references.ParameterReference
import com.attafitamim.kabin.compiler.sql.syntax.SQLBuilder
import com.attafitamim.kabin.compiler.sql.syntax.SQLQuery
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
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.toParameterReferences
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.toReference
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.toReferences
import com.attafitamim.kabin.compiler.sql.utils.sql.buildSQLQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.getFlatColumns
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.dao.DaoActionSpec
import com.attafitamim.kabin.specs.dao.DaoFunctionSpec
import com.attafitamim.kabin.specs.dao.DaoParameterSpec
import com.attafitamim.kabin.specs.dao.DataTypeSpec
import com.attafitamim.kabin.specs.entity.EntitySpec

fun SQLQuery.getParameterReferences(): List<ParameterReference> = when (this) {
    is SQLQuery.Columns -> columns.toParameterReferences()
    is SQLQuery.Parameters -> parameters.toReferences()
    is SQLQuery.Raw -> listOf(rawQueryParameter.toReference())
}


fun DaoActionSpec.EntityAction.getSQLQuery(
    returnEntitySpec: EntitySpec?
): SQLQuery.Columns {
    val actualEntitySpec = requireNotNull(entitySpec ?: returnEntitySpec)
    return when (this) {
        is DaoActionSpec.Delete -> getSQLQuery(actualEntitySpec)
        is DaoActionSpec.Insert -> getSQLQuery(actualEntitySpec)
        is DaoActionSpec.Update -> getSQLQuery(actualEntitySpec)
        is DaoActionSpec.Upsert -> getSQLQuery(actualEntitySpec)
    }
}

fun DaoActionSpec.QueryAction.getSQLQuery(
    functionSpec: DaoFunctionSpec
): SQLQuery = when (this) {
    is DaoActionSpec.Query -> getSQLQuery(functionSpec)
    is DaoActionSpec.RawQuery -> getSQLQuery(functionSpec.parameters)
}

fun DaoActionSpec.Query.getSQLQuery(
    functionSpec: DaoFunctionSpec
): SQLQuery.Parameters {
    val queryParts = value.split(" ")
    val parametersMap = functionSpec.parameters
        .associateBy(DaoParameterSpec::name)

    val sortedParameters = ArrayList<DaoParameterSpec>()
    val query = buildSQLQuery {
        queryParts.forEach { part ->
            if (part.startsWith(SQLSyntax.Sign.VALUE_PREFIX)) {
                val parameterName = part.replace(SQLSyntax.Sign.VALUE_PREFIX, "")
                    .replace(SQLSyntax.Sign.STATEMENT_SEPARATOR, "")
                    .replace(SQLSyntax.Sign.FUNCTION_OPEN_PARENTHESES, "")
                    .replace(SQLSyntax.Sign.FUNCTION_CLOSE_PARENTHESES, "")
                    .trim()

                val parameter = parametersMap[parameterName]
                when (parameter?.typeSpec?.dataType) {
                    is DataTypeSpec.DataType.Class -> {
                        VALUE
                    }

                    is DataTypeSpec.DataType.Collection -> {
                        append("\$${parameterName}Indexes")
                    }

                    is DataTypeSpec.DataType.Entity,
                    is DataTypeSpec.DataType.Compound,
                    is DataTypeSpec.DataType.Stream,
                    null -> error("not supported")
                }

                sortedParameters.add(parameter)
            } else {
                append(part)
            }
        }
    }

    return SQLQuery.Parameters(query, sortedParameters.size, sortedParameters)
}

fun DaoActionSpec.RawQuery.getSQLQuery(
    parameters: List<DaoParameterSpec>
): SQLQuery.Raw {
    val queryParameter = parameters.first()
    return SQLQuery.Raw(queryParameter)
}

fun getSelectSQLQuery(
    entitySpec: EntitySpec,
    column: ColumnSpec
): SQLQuery.Columns {
    val columns = setOf(column)
    val parameters = setOf(column.name)
    val query = buildSQLQuery {
        SELECT; ALL; FROM(entitySpec.tableName); WHERE.equalParameters(parameters)
    }

    return SQLQuery.Columns(query, parameters.size, columns)
}

fun getSelectSQLQuery(
    entitySpec: EntitySpec,
    columns: List<ColumnSpec>
): SQLQuery.Columns {
    val flatColumns = getFlatColumns(columns)
    val parameters = flatColumns.map(ColumnSpec::name)
    val query = buildSQLQuery {
        SELECT; ALL; FROM(entitySpec.tableName); WHERE.equalParameters(parameters)
    }

    return SQLQuery.Columns(query, parameters.size, flatColumns)
}

fun DaoActionSpec.Delete.getSQLQuery(
    actualEntitySpec: EntitySpec
): SQLQuery.Columns {
    val entity = entitySpec ?: actualEntitySpec
    val parameters = entity.primaryKeys
    val query = buildSQLQuery {
        DELETE; FROM(entity.tableName); WHERE.equalParameters(parameters)
    }

    val columns = entity.columns.filter { columnSpec ->
        parameters.contains(columnSpec.name)
    }

    return SQLQuery.Columns(query, parameters.size, columns)
}

fun DaoActionSpec.Insert.getSQLQuery(
    actualEntitySpec: EntitySpec
): SQLQuery.Columns {
    val parameters = getFlatColumns(actualEntitySpec.columns)
        .map(ColumnSpec::name)

    val query = buildSQLQuery {
        INSERT.or(onConflict); INTO(actualEntitySpec.tableName); VALUES.parameters(parameters)
    }

    return SQLQuery.Columns(query, parameters.size, actualEntitySpec.columns)
}

fun DaoActionSpec.Update.getSQLQuery(
    actualEntitySpec: EntitySpec
): SQLQuery.Columns {
    val parameters = getFlatColumns(actualEntitySpec.columns)
        .map(ColumnSpec::name)

    val primaryKeys = actualEntitySpec.primaryKeys
    val query = buildSQLQuery {
        UPDATE.or(onConflict)(actualEntitySpec.tableName); SET.namedParameters(parameters)
        WHERE.equalParameters(actualEntitySpec.primaryKeys)
    }

    val parametersSize = parameters.size + primaryKeys.size
    return SQLQuery.Columns(query, parametersSize, actualEntitySpec.columns)
}

fun DaoActionSpec.Upsert.getSQLQuery(
    actualEntitySpec: EntitySpec
): SQLQuery.Columns {
    val entity = entitySpec ?: actualEntitySpec
    val parameters = getFlatColumns(entity.columns)
        .map(ColumnSpec::name)

    val query = buildSQLQuery {
        INSERT; OR; REPLACE; INTO(actualEntitySpec.tableName); VALUES.parameters(parameters)
    }

    return SQLQuery.Columns(query, parameters.size, actualEntitySpec.columns)
}

fun SQLBuilder.equalParameters(parameters: Collection<String>) {
    parameters.forEach { key ->
        append(key); EQUALS; VALUE

        if (parameters.last() != key) {
            AND
        }
    }
}

fun SQLBuilder.namedParameters(parameters: List<String>) {
    parameters.forEachIndexed { index, column ->
        val isLastStatement = index == parameters.lastIndex
        appendStatement(!isLastStatement) {
            append(column); EQUALS; VALUE
        }
    }
}

fun SQLBuilder.parameters(parameters: List<String>) {
    wrap {
        parameters.forEachIndexed { index, _ ->
            val isLastStatement = index == parameters.lastIndex
            appendStatement(!isLastStatement) {
                VALUE
            }
        }
    }
}

fun SQLBuilder.or(onConflictStrategy: OnConflictStrategy?): SQLBuilder =
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
