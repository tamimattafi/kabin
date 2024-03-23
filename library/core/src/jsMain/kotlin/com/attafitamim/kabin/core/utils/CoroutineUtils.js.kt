package com.attafitamim.kabin.core.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val Dispatchers.IO: CoroutineDispatcher
    get() = Default
