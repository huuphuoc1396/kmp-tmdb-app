package com.banhmilabs.tmdb.service.title.data

import com.banhmilabs.tmdb.core.common.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Runs at most one refresh per key (a title list, a title's details); a later call for the same key awaits the running
 * one. Refreshes run in [scope], so they complete even if the caller is cancelled.
 */
internal class KeyedRefreshes<K : Any>(private val scope: CoroutineScope) {
    /** Refreshes in progress. Guarded by [mutex]. */
    private val running = mutableMapOf<K, Deferred<AppResult<Unit>>>()
    private val mutex = Mutex()

    /** Awaits the refresh running for [key]; otherwise launches [refresh] if [needed], else returns `Success`. */
    suspend fun joinOrLaunch(
        key: K,
        needed: suspend () -> Boolean,
        refresh: suspend () -> AppResult<Unit>,
    ): AppResult<Unit> {
        val job = mutex.withLock {
            running[key] ?: if (needed()) launch(key, refresh) else null
        }
        return job?.await() ?: AppResult.Success(Unit)
    }

    /** Call with [mutex] held. */
    private fun launch(key: K, refresh: suspend () -> AppResult<Unit>): Deferred<AppResult<Unit>> = scope.async {
        try {
            refresh()
        } finally {
            mutex.withLock { running -= key }
        }
    }.also { running[key] = it }
}
