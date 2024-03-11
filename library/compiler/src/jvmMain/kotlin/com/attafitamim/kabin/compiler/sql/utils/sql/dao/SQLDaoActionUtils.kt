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
import com.google.devtools.ksp.processing.KSPLogger

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
    functionSpec: DaoFunctionSpec,
    logger: KSPLogger
): SQLQuery = when (this) {
    is DaoActionSpec.Query -> getSQLQuery(functionSpec, logger)
    is DaoActionSpec.RawQuery -> getSQLQuery(functionSpec.parameters)
}

private val sqlSpecialCharacters =
    listOf('$', '(', ')', '[', ']', ' ', ',', ';', ':', '*', '.')

fun DaoActionSpec.Query.getSQLQuery(
    functionSpec: DaoFunctionSpec,
    logger: KSPLogger
): SQLQuery.Parameters {
    val parametersMap = functionSpec.parameters
        .associateBy(DaoParameterSpec::name)

    val sortedParameters = ArrayList<DaoParameterSpec>()
    val cleanQuery = value.trim().replace(Regex("\\s+"), " ")

    fun getSQLParameterStatement(name: String): String {
        val parameter = parametersMap[name] ?: logger.throwException(
            "Can't find parameter with the name: $name",
            functionSpec.declaration
        )

        sortedParameters.add(parameter)
        return when (parameter.typeSpec.dataType) {
            is DataTypeSpec.DataType.Class -> "?"
            is DataTypeSpec.DataType.Collection -> "\$${name}Indexes"
            else -> logger.throwException(
                "Parameters with type ${parameter.declaration.type} are not supported here",
                parameter.declaration
            )
        }
    }

    val currentParameter = StringBuilder()
    var isBuildingParameter = false
    val query = buildString {
        cleanQuery.forEachIndexed { index, char ->
            val isLastChar = index == cleanQuery.lastIndex
            val isParameterEnd = char in sqlSpecialCharacters
            val isParameterStart = char == ':'

            when {
                isParameterStart -> {
                    isBuildingParameter = true
                    return@forEachIndexed
                }

                isBuildingParameter -> {
                    if (!isParameterEnd) {
                        currentParameter.append(char)

                        if (!isLastChar) {
                            return@forEachIndexed
                        }
                    }

                    isBuildingParameter = false
                    val parameterName = currentParameter.toString()
                    currentParameter.clear()

                    val parameterStatement = getSQLParameterStatement(parameterName)
                    append(parameterStatement)

                    if (isParameterEnd) {
                        append(char)
                    }
                }

                else -> {
                    append(char)
                }
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
        INSERT.or(onConflict); INTO(actualEntitySpec.tableName); VALUES.variableParameters(parameters)
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
        INSERT; OR; REPLACE; INTO(actualEntitySpec.tableName); VALUES.variableParameters(parameters)
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
