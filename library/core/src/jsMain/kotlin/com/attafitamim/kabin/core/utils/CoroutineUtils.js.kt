package com.attafitamim.kabin.core.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

internal actual val Dispatchers.IO: CoroutineDispatcher
    get() = Default

@OptIn(ExperimentalCoroutinesApi::class)
internal actual fun createSingleThreadDispatcher(): CoroutineDispatcher =
    Dispatchers.IO.limitedParallelism(1)