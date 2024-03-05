package com.attafitamim.kabin.core.utils

import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher

internal actual fun createSingleThreadDispatcher(): CoroutineDispatcher =
    Executors.newSingleThreadExecutor().asCoroutineDispatcher()

internal actual val Dispatchers.IO: CoroutineDispatcher
    get() = IO