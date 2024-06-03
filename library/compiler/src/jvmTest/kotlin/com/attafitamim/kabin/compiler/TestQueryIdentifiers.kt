package com.attafitamim.kabin.compiler

import com.attafitamim.kabin.compiler.sql.utils.poet.sqldelight.getUniqueQueryIdentifier
import java.util.UUID
import org.junit.Assert
import org.junit.Test

class TestQueryIdentifiers {

    @Test
    fun `unique identifiers for random queries`() {
        IntRange(0, MAX_CYCLES).forEach { index ->
            val userId = UUID.randomUUID().toString()
            val randomQuery = "SELECT * FROM UserEntity WHERE id = $userId"
            val queryIdentifier = randomQuery.getUniqueQueryIdentifier()
            Assert.assertNotNull(
                "$index - Couldn't find a unique query identifier for random query $randomQuery",
                queryIdentifier
            )
        }
    }

    @Test
    fun `unique identifier for fixed query`() {
        val userId = UUID.randomUUID().toString()
        val fixedQuery = "SELECT * FROM UserEntity WHERE id = $userId"

        IntRange(0, MAX_CYCLES).forEach { index ->
            val queryIdentifier = fixedQuery.getUniqueQueryIdentifier()
            Assert.assertNotNull(
                "$index - Couldn't find a unique query identifier for fixed query $fixedQuery",
                queryIdentifier
            )
        }
    }

    private companion object {
        const val MAX_CYCLES = 1000000
    }
}