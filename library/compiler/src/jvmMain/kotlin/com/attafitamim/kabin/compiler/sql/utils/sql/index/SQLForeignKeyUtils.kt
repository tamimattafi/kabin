package com.attafitamim.kabin.compiler.sql.utils.sql.index

import com.attafitamim.kabin.annotations.ForeignKey
import com.attafitamim.kabin.compiler.sql.syntax.SQLBuilder
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.CASCADE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.DEFAULT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.DEFERRABLE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.DEFERRED
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.DELETE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.FOREIGN_KEY
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.IMMEDIATE
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.INITIALLY
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.NO_ACTION
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.NULL
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.ON
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.REFERENCES
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.RESTRICT
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.SET
import com.attafitamim.kabin.compiler.sql.syntax.SQLSyntax.UPDATE
import com.attafitamim.kabin.specs.relation.ForeignKeySpec

fun SQLBuilder.appendForeignKeyDefinition(
    foreignKeySpec: ForeignKeySpec,
    isLastStatement: Boolean
) = appendStatement(!isLastStatement) {
    val parentColumns = requireNotNull(foreignKeySpec.parentColumns)
    val childColumns = requireNotNull(foreignKeySpec.childColumns)

    FOREIGN_KEY.wrap {
        childColumns.forEachIndexed { index, childColumn ->
            val isLastKey = index == childColumns.lastIndex
            appendStatement(!isLastKey) {
                append(childColumn)
            }
        }
    }

    REFERENCES(foreignKeySpec.entitySpec.tableName).wrap {
        parentColumns.forEachIndexed { index, parentColumn ->
            val isLastKey = index == parentColumns.lastIndex
            appendStatement(!isLastKey) {
                append(parentColumn)
            }
        }
    }

    ON; UPDATE(foreignKeySpec.onUpdate)
    ON; DELETE(foreignKeySpec.onDelete)

    DEFERRABLE; INITIALLY; if (foreignKeySpec.deferred) DEFERRED else IMMEDIATE
}

operator fun SQLBuilder.invoke(action: ForeignKey.Action?) = when (action) {
    ForeignKey.Action.RESTRICT -> RESTRICT
    ForeignKey.Action.SET_NULL -> { SET; NULL }
    ForeignKey.Action.SET_DEFAULT -> { SET; DEFAULT }
    ForeignKey.Action.CASCADE -> CASCADE
    ForeignKey.Action.NO_ACTION,
    null -> NO_ACTION
}
