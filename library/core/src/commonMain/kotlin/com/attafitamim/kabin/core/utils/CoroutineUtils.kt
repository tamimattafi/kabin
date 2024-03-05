package com.attafitamim.kabin.core.utils
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal expect val Dispatchers.IO: CoroutineDispatcher

internal expect fun createSingleThreadDispatcher(): CoroutineDispatcher

internal suspend fun <T> withContextIO(
    block: suspend CoroutineScope.() -> T
): T = withContext(coroutineContext + Dispatchers.IO, block)
