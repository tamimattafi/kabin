package com.attafitamim.kabin.compiler.sql.syntax

object SQLSyntax {

    val SQLBuilder.CREATE get() = append("CREATE")
    val SQLBuilder.DROP get() = append("DROP")
    val SQLBuilder.EXISTS get() = append("EXISTS")
    val SQLBuilder.NOT get() = append("NOT")
    val SQLBuilder.AND get() = append("AND")
    val SQLBuilder.OR get() = append("OR")
    val SQLBuilder.IF get() = append("IF")
    val SQLBuilder.WHERE get() = append("WHERE")
    val SQLBuilder.FROM get() = append("FROM")
    val SQLBuilder.INTO get() = append("INTO")
    val SQLBuilder.VALUES get() = append("VALUES")
    val SQLBuilder.DEFAULT get() = append("DEFAULT")
    val SQLBuilder.AUTO_INCREMENT get() = append("AUTOINCREMENT")
    val SQLBuilder.PRIMARY_KEY get() = append("PRIMARY KEY")
    val SQLBuilder.INDEX get() = append("INDEX")
    val SQLBuilder.UNIQUE get() = append("UNIQUE")
    val SQLBuilder.TABLE get() = append("TABLE")
    val SQLBuilder.NULL get() = append("NULL")
    val SQLBuilder.FOREIGN_KEY get() = append("FOREIGN KEY")
    val SQLBuilder.REFERENCES get() = append("REFERENCES")
    val SQLBuilder.ON get() = append("ON")
    val SQLBuilder.UPDATE get() = append("UPDATE")
    val SQLBuilder.DELETE get() = append("DELETE")
    val SQLBuilder.SELECT get() = append("SELECT")
    val SQLBuilder.ALL get() = append("*")
    val SQLBuilder.INSERT get() = append("INSERT")
    val SQLBuilder.CONFLICT get() = append("CONFLICT")
    val SQLBuilder.NO_ACTION get() = append("NO ACTION")
    val SQLBuilder.CASCADE get() = append("CASCADE")
    val SQLBuilder.RESTRICT get() = append("RESTRICT")
    val SQLBuilder.SET get() = append("SET")
    val SQLBuilder.DEFERRABLE get() = append("DEFERRABLE")
    val SQLBuilder.INITIALLY get() = append("INITIALLY")
    val SQLBuilder.DEFERRED get() = append("DEFERRED")
    val SQLBuilder.ASC get() = append("ASC")
    val SQLBuilder.DESC get() = append("DESC")
    val SQLBuilder.ROLLBACK get() = append("ROLLBACK")
    val SQLBuilder.ABORT get() = append("ABORT")
    val SQLBuilder.FAIL get() = append("FAIL")
    val SQLBuilder.IGNORE get() = append("IGNORE")
    val SQLBuilder.REPLACE get() = append("REPLACE")
    val SQLBuilder.EQUALS get() = append("=")
    val SQLBuilder.VALUE get() = append("?")

    object Sign {
        const val SEPARATOR = " "
        const val STATEMENT_SEPARATOR = ","
        const val VALUE_ESCAPING = "'"
        const val FUNCTION_OPEN_PARENTHESES = "("
        const val FUNCTION_CLOSE_PARENTHESES = ")"
        const val NAME_SEPARATOR = "_"
        const val VALUE_PREFIX = ":"
    }
}