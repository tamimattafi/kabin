package com.attafitamim.kabin.compiler.sql.syntax

const val SQL_SEPARATOR = " "
const val SQL_STATEMENT_SEPARATOR = ",\n"

const val SQL_VALUE_ESCAPING = "'"

const val SQL_CREATE_TABLE_TEMPLATE = """
CREATE TABLE IF NOT EXISTS %s (
    %s
)
"""

const val SQL_COLUMN_PRIMARY_KEY = "PRIMARY KEY"
const val SQL_COLUMN_AUTO_GENERATE = "AUTOINCREMENT"
const val SQL_COLUMN_NOT_NULL = "NOT NULL"
const val SQL_COLUMN_DEFAULT_VALUE = "DEFAULT"