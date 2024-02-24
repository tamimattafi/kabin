package com.attafitamim.kabin.compiler.sql.syntax

object SQLSyntax {

    object Sign {
        const val SEPARATOR = " "
        const val STATEMENT_SEPARATOR = ","
        const val VALUE_ESCAPING = "'"
        const val FUNCTION_OPEN_PARENTHESES = "("
        const val FUNCTION_CLOSE_PARENTHESES = ")"
    }

    object Operator {
        const val CREATE = "CREATE"
        const val IF = "IF"
        const val NOT = "NOT"
        const val EXITS = "EXISTS"
    }

    object Type {
        const val TABLE = "TABLE"
        const val INDEX = "INDEX"
        const val PRIMARY_KEY = "PRIMARY KEY"
    }

    object Value {
        const val NULL = "NULL"
    }

    object Column {
        const val AUTO_GENERATE = "AUTOINCREMENT"
        const val DEFAULT_VALUE = "DEFAULT"
    }
}
