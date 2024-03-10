package com.attafitamim.kabin.core.dao

import app.cash.sqldelight.ExecutableQuery
import app.cash.sqldelight.Query
import app.cash.sqldelight.SuspendingTransacter
import app.cash.sqldelight.SuspendingTransactionWithReturn
import app.cash.sqldelight.SuspendingTransactionWithoutReturn
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneNotNull
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.attafitamim.kabin.core.utils.createSingleThreadDispatcher
import com.attafitamim.kabin.core.utils.withContextIO
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map as mapFlow
import kotlinx.coroutines.withContext

private val transactionDispatcher = createSingleThreadDispatcher()

interface KabinDao<T : SuspendingTransacter> {

    val queries: T

    suspend fun transaction(
        body: suspend SuspendingTransactionWithoutReturn.() -> Unit
    ) = withSafeDispatcher {
        queries.transaction(body = body)
    }

    suspend fun <R> transactionWithResult(
        body: suspend SuspendingTransactionWithReturn<R>.() -> R
    ): R = withSafeDispatcher {
        queries.transactionWithResult(bodyWithReturn = body)
    }

    suspend fun <T : Any> ExecutableQuery<T>.awaitAsListIO(): List<T> = withContextIO {
        awaitAsList()
    }

    suspend fun <T : Any> ExecutableQuery<T>.awaitAsOneNotNullIO(): T = withContextIO {
        awaitAsOne()
    }

    suspend fun <T : Any> ExecutableQuery<T>.awaitAsOneOrNullIO(): T? = withContextIO {
        awaitAsOneOrNull()
    }

    suspend fun <T : Any> Query<T>.asFlowIOList(): Flow<List<T>> = applyIOContext { originalContext ->
        asFlow().mapToList(originalContext)
    }

    suspend fun <T : Any> Query<T>.asFlowIONullable(): Flow<T?> = applyIOContext { originalContext ->
        asFlow().mapToOneOrNull(originalContext)
    }

    suspend fun <T : Any> Query<T>.asFlowIONotNull(): Flow<T> = applyIOContext { originalContext ->
        asFlow().mapToOneNotNull(originalContext)
    }

    private suspend fun <T> applyIOContext(onIoContext: suspend (CoroutineContext) -> T): T {
        val originalContext = coroutineContext

        return withContextIO {
            onIoContext.invoke(originalContext)
        }
    }

    /**
     * As SQLDelight requires all transactions to be executed on one thread,
     * consider using only this method with transactions flow
     */
    private suspend fun <R> withSafeDispatcher(
        call: suspend CoroutineScope.() -> R,
    ) = withContext(transactionDispatcher) {
        call()
    }

    fun <T, R> Flow<T>.map(transform: suspend (value: T) -> R): Flow<R> = mapFlow(transform)
}