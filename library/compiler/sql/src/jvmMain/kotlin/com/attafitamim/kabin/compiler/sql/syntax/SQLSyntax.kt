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
        val SQLBuilder.CREATE get() = append("CREATE")
        val SQLBuilder.EXITS get() = append("EXITS")
        val SQLBuilder.NOT get() = append("NOT")
        val SQLBuilder.IF get() = append("IF")
        val SQLBuilder.DEFAULT get() = append("DEFAULT")
        val SQLBuilder.AUTO_INCREMENT get() = append("AUTOINCREMENT")
    }

    object Value {
        val SQLBuilder.NULL get() = append("NULL")
    }

    object Type {
        val SQLBuilder.PRIMARY_KEY get() = append("PRIMARY KEY")
        val SQLBuilder.INDEX get() = append("INDEX")
        val SQLBuilder.TABLE get() = append("TABLE")
    }
}