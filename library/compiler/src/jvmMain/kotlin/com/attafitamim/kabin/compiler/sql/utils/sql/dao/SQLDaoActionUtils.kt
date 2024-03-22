package com.attafitamim.kabin.compiler.sql.utils.sql.dao

import com.attafitamim.kabin.annotations.dao.OnConflictStrategy
import com.attafitamim.kabin.compiler.sql.generator.references.ParameterReference
import com.attafitamim.kabin.compiler.sql.syntax.SQLBuilder
import com.attafitamim.kabin.compiler.sql.syntax.SQLQuery
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
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.toQueryParameterReferences
import com.attafitamim.kabin.compiler.sql.utils.poet.dao.toReference
import com.attafitamim.kabin.compiler.sql.utils.sql.buildSQLQuery
import com.attafitamim.kabin.compiler.sql.utils.sql.entity.getFlatColumns
import com.attafitamim.kabin.processor.utils.throwException
import com.attafitamim.kabin.specs.column.ColumnSpec
import com.attafitamim.kabin.specs.dao.DaoActionSpec
import com.attafitamim.kabin.specs.dao.DaoFunctionSpec
import com.attafitamim.kabin.specs.dao.DaoParameterSpec
import com.attafitamim.kabin.specs.dao.DataTypeSpec
import com.attafitamim.kabin.specs.entity.EntitySpec
import com.google.devtools.ksp.processing.KSPLogger

fun SQLQuery.getParameterReferences(): Collection<ParameterReference> = when (this) {
    is SQLQuery.Columns -> columns.toParameterReferences()
    is SQLQuery.Parameters -> queryParameters.toQueryParameterReferences()
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
    functionSpec: DaoFunctionSpec,
    logger: KSPLogger
): SQLQuery = when (this) {
    is DaoActionSpec.Query -> getSQLQuery(functionSpec, logger)
    is DaoActionSpec.RawQuery -> getSQLQuery(functionSpec.parameters)
}

private val sqlSpecialCharacters =
    setOf('$', '(', ')', '[', ']', ' ', ',', ';', '*', '.')

private val sqlActions = setOf(
    "SELECT", "DELETE", "UPDATE", "INSERT"
)

private val conflictStrategies = setOf(
    "ROLLBACK", "ABORT", "FAIL", "IGNORE", "REPLACE"
)

// TODO: refactor this
fun DaoActionSpec.Query.getSQLQuery(
    functionSpec: DaoFunctionSpec,
    logger: KSPLogger
): SQLQuery.Parameters {
    val parametersMap = functionSpec.parameters
        .associateBy(DaoParameterSpec::name)

    val sortedQueryParameters = ArrayList<SQLQuery.Parameters.QueryParameter>()
    val cleanQuery = value.trim().replace(Regex("\\s+"), " ")

    val parameterPrefix = ":"

    var actionKeyword: String? = null
    var previousKeyword: String? = null
    val currentKeyword = StringBuilder()

    val queriedKeys = LinkedHashSet<String>()
    val mutatedKeys = LinkedHashSet<String>()

    fun getSQLParameterStatement(name: String): String {
        val parameterSpec = parametersMap[name] ?: logger.throwException(
            "Can't find parameter with the name: $name",
            functionSpec.declaration
        )

        val queryParameter = SQLQuery.Parameters.QueryParameter(
            parameterSpec,
            requireNotNull(actionKeyword),
            requireNotNull(previousKeyword)
        )

        sortedQueryParameters.add(queryParameter)
        return when (parameterSpec.typeSpec.dataType) {
            is DataTypeSpec.DataType.Class -> "?"
            is DataTypeSpec.DataType.Collection -> {
                if (previousKeyword == "IN") {
                    "\$${name}Arguments"
                } else {
                    "\$${name}Parameter"
                }
            }

            else -> logger.throwException(
                "Parameters with type ${parameterSpec.declaration.type} are not supported here",
                parameterSpec.declaration
            )
        }
    }

    fun handleKeywords(
        actionKeyword: String,
        previousKeyword: String,
        currentKeyword: String
    ) {
        when {
            actionKeyword == "SELECT" && previousKeyword == "FROM" -> {
                queriedKeys.add(currentKeyword)
            }

            actionKeyword == "DELETE" && previousKeyword == "FROM" -> {
                mutatedKeys.add(currentKeyword)
            }

            actionKeyword == "INSERT" && previousKeyword == "INTO" -> {
                mutatedKeys.add(currentKeyword)
            }

            actionKeyword == "UPDATE"
                    && previousKeyword == "UPDATE"
                    && currentKeyword != "ON"
                    && !conflictStrategies.contains(currentKeyword) -> {
                        mutatedKeys.add(currentKeyword)
                    }

            actionKeyword == "UPDATE" && conflictStrategies.contains(previousKeyword) -> {
                mutatedKeys.add(currentKeyword)
            }
        }
    }

    fun StringBuilder.appendKeyword(keyword: String) {
        val currentActionKeyword = actionKeyword
        val currentPreviousKeyword = previousKeyword

        when {
            sqlActions.contains(keyword) -> {
                actionKeyword = keyword
            }

            !currentActionKeyword.isNullOrBlank() && !currentPreviousKeyword.isNullOrBlank() -> {
                handleKeywords(
                    currentActionKeyword,
                    currentPreviousKeyword,
                    keyword
                )
            }
        }

        previousKeyword = keyword
        append(keyword)
    }

    fun StringBuilder.appendCurrentKeyword() {
        val keyword = currentKeyword.toString()
        currentKeyword.clear()

        if (keyword.startsWith(parameterPrefix)) {
            val parameter = keyword.removePrefix(parameterPrefix)
            val statement = getSQLParameterStatement(parameter)
            append(statement)
        } else {
            appendKeyword(keyword)
        }
    }

    val query = buildString {
        cleanQuery.forEachIndexed { index, char ->
            if (sqlSpecialCharacters.contains(char)) {
                appendCurrentKeyword()
                append(char)
            } else {
                currentKeyword.append(char)
                if (index == cleanQuery.lastIndex) {
                    appendCurrentKeyword()
                }
            }
        }
    }

    return SQLQuery.Parameters(
        query,
        sortedQueryParameters.size,
        sortedQueryParameters,
        mutatedKeys,
        queriedKeys
    )
}

fun DaoActionSpec.RawQuery.getSQLQuery(
    parameters: List<DaoParameterSpec>
): SQLQuery.Raw {
    val queryParameter = parameters.first()
    val observedKeys = observedEntities?.map(EntitySpec::tableName)?.toSet().orEmpty()
    return SQLQuery.Raw(queryParameter, observedKeys)
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

    return SQLQuery.Columns(
        query,
        parameters.size,
        columns,
        mutatedKeys = emptySet(),
        queriedKeys = setOf(entitySpec.tableName)
    )
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

    return SQLQuery.Columns(
        query,
        parameters.size,
        columns,
        mutatedKeys = setOf(entity.tableName),
        queriedKeys = emptySet()
    )
}

fun DaoActionSpec.Insert.getSQLQuery(
    actualEntitySpec: EntitySpec
): SQLQuery.Columns {
    val entity = entitySpec ?: actualEntitySpec
    val parameters = getFlatColumns(entity.columns)
        .map(ColumnSpec::name)

    val query = buildSQLQuery {
        INSERT.or(onConflict); INTO(entity.tableName); VALUES.variableParameters(parameters)
    }

    return SQLQuery.Columns(
        query,
        parameters.size,
        entity.columns,
        mutatedKeys = setOf(entity.tableName),
        queriedKeys = emptySet()
    )
}

fun DaoActionSpec.Update.getSQLQuery(
    actualEntitySpec: EntitySpec
): SQLQuery.Columns {
    val entity = entitySpec ?: actualEntitySpec
    val parameters = getFlatColumns(entity.columns)
        .map(ColumnSpec::name)

    val primaryKeys = entity.primaryKeys
    val query = buildSQLQuery {
        UPDATE.or(onConflict)(entity.tableName); SET.namedParameters(parameters)
        WHERE.equalParameters(entity.primaryKeys)
    }

    val parametersSize = parameters.size + primaryKeys.size
    return SQLQuery.Columns(
        query,
        parametersSize,
        entity.columns,
        mutatedKeys = setOf(entity.tableName),
        queriedKeys = emptySet()
    )
}

fun DaoActionSpec.Upsert.getSQLQuery(
    actualEntitySpec: EntitySpec
): SQLQuery.Columns {
    val entity = entitySpec ?: actualEntitySpec
    val parameters = getFlatColumns(entity.columns)
        .map(ColumnSpec::name)

    val query = buildSQLQuery {
        INSERT; OR; REPLACE; INTO(entity.tableName); VALUES.variableParameters(parameters)
    }

    return SQLQuery.Columns(
        query,
        parameters.size,
        actualEntitySpec.columns,
        mutatedKeys = setOf(entity.tableName),
        queriedKeys = emptySet()
    )
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

fun SQLBuilder.variableParameters(parameters: List<String>) {
    wrap {
        parameters.forEachIndexed { index, _ ->
            val isLastStatement = index == parameters.lastIndex
            appendStatement(!isLastStatement) {
                VALUE
            }
        }
    }
}

fun SQLBuilder.parameters(parameters: List<String>) {
    wrap {
        parameters.forEachIndexed { index, parameter ->
            val isLastStatement = index == parameters.lastIndex
            appendStatement(!isLastStatement) {
                append(parameter)
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
