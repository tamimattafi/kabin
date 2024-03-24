package com.attafitamim.kabin.core.utils

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.db.QueryResult

suspend fun <T : Any> ExecutableQuery<T>.awaitAll(): List<T> = execute { cursor ->
    val first = cursor.next()
    val result = mutableListOf<T>()

    // If the cursor isn't async, we want to preserve the blocking semantics and execute it synchronously
    when (first) {
        is QueryResult.AsyncValue -> {
            QueryResult.AsyncValue {
                if (first.await()) result.add(mapper(cursor)) else return@AsyncValue result
                while (cursor.next().await()) result.add(mapper(cursor))
                result
            }
        }

        is QueryResult.Value -> {
            if (first.value) result.add(mapper(cursor)) else return@execute QueryResult.Value(result)
            while (cursor.next().value) result.add(mapper(cursor))
            QueryResult.Value(result)
        }
    }
}.await()

suspend fun <T : Any> ExecutableQuery<T>.awaitFirst(): T {
    return awaitFirstOrNull()
        ?: throw NullPointerException("ResultSet returned null for $this")
}

suspend fun <T : Any> ExecutableQuery<T>.awaitFirstOrNull(): T? = execute { cursor ->
    val next = cursor.next()

    // If the cursor isn't async, we want to preserve the blocking semantics and execute it synchronously
    when (next) {
        is QueryResult.AsyncValue -> {
            QueryResult.AsyncValue {
                if (!next.await()) return@AsyncValue null
                mapper(cursor)
            }
        }

        is QueryResult.Value -> {
            if (!next.value) return@execute QueryResult.Value(null)
            val value = mapper(cursor)
            QueryResult.Value(value)
        }
    }
}.await()