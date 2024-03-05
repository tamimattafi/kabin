package com.attafitamim.kabin.query

internal object SQLiteQueryUtils {


    /**
     * Transfers this array to SQLite elements separated by a comma (','). This can be used inside SQLite arrays, or selection columns etc..
     * @return Returns a string that contains elements of this array joined by a comma (','). For example, ("James", "Carl", "Hannah") will be
     * look like "James, Carl, Hannah"
     *
     * Note that parentheses are not appended by default
     *
     */
    internal fun Array<out Any>.toSQLiteElements(): String =
        joinToString(separator = SQLiteSyntax.ELEMENT_SEPARATOR)


    /**
     * Transfers this array to SQLite elements separated by a comma (','). This can be used inside SQLite arrays, or selection columns etc..
     * @return Returns a string that contains elements of this array joined by a comma (','). For example, ("James", "Carl", "Hannah") will be
     * look like "James, Carl, Hannah"
     *
     * Note that parentheses are appended by default
     *
     */
    internal fun Array<out Any>.toContainedSQLiteElements(): String = joinToString(
        separator = SQLiteSyntax.ELEMENT_SEPARATOR,
        prefix = SQLiteSyntax.OPEN_PARENTHESES,
        postfix = SQLiteSyntax.CLOSE_PARENTHESES
    ) { value ->
        "'$value'"
    }
}
