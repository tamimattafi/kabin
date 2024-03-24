package com.attafitamim.kabin.core.dao

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.Query
import app.cash.sqldelight.SuspendingTransacter
import app.cash.sqldelight.SuspendingTransactionWithReturn
import app.cash.sqldelight.SuspendingTransactionWithoutReturn
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.attafitamim.kabin.core.utils.IO
import com.attafitamim.kabin.core.utils.awaitAll
import com.attafitamim.kabin.core.utils.awaitFirst
import com.attafitamim.kabin.core.utils.awaitFirstOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

interface KabinDao<T : SuspendingTransacter> {

    val queries: T

    suspend fun transaction(
        body: suspend SuspendingTransactionWithoutReturn.() -> Unit
    ) = withContextIO {
        queries.transaction(body = body)
    }

    suspend fun <R> transactionWithResult(
        body: suspend SuspendingTransactionWithReturn<R>.() -> R
    ): R = withContextIO {
        queries.transactionWithResult(bodyWithReturn = body)
    }

    suspend fun <T : Any> ExecutableQuery<T>.awaitAsListIO(): List<T> = withContextIO {
        awaitAll()
    }

    suspend fun <T : Any> ExecutableQuery<T>.awaitAsOneNotNullIO(): T = withContextIO {
        awaitFirst()
    }

    suspend fun <T : Any> ExecutableQuery<T>.awaitAsOneOrNullIO(): T? = withContextIO {
        awaitFirstOrNull()
    }

    fun <T : Any> Query<T>.asFlowIOList(): Flow<List<T>> =
        asFlow().mapToList(Dispatchers.IO)

    fun <T : Any> Query<T>.asFlowIONullable(): Flow<T?> =
        asFlow().mapToOneOrNull(Dispatchers.IO)

    fun <T : Any> Query<T>.asFlowIO(): Flow<T> =
        asFlow().mapToOne(Dispatchers.IO)

    fun <T, R> Flow<T>.mapIO(transform: suspend (value: T) -> R): Flow<R> = map { value ->
        withContextIO {
            transform(value)
        }
    }

    suspend fun <T, R> Collection<T>.mapIO(transform: suspend (value: T) -> R): List<R> = map { value ->
        withContextIO {
            transform(value)
        }
    }

    private suspend fun <T> withContextIO(onIoContext: suspend CoroutineScope.() -> T): T =
        withContext(Dispatchers.IO, onIoContext)
}