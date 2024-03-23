package com.attafitamim.kabin.core.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

internal actual val Dispatchers.IO: CoroutineDispatcher
    get() = IO