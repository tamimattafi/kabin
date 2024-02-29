package com.attafitamim.kabin.core.utils

import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

internal expect fun createSingleThreadDispatcher(): CoroutineDispatcher

internal suspend fun <T> withContextIO(
    block: suspend CoroutineScope.() -> T
): T = withContext(coroutineContext + Dispatchers.IO, block)
