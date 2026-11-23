package com.banhmilabs.tmdb.core.testing

import com.banhmilabs.tmdb.core.common.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher

class TestDispatcherProvider(override val io: CoroutineDispatcher) : DispatcherProvider
