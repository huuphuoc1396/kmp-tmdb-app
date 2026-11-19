package com.banhmilabs.tmdb.service.title.data

import com.banhmilabs.tmdb.core.common.AppResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Runs at most one refresh per title list; a later call for the same list awaits the running one. Refreshes run in
 * [scope], so they complete even if the caller is cancelled.
 */
internal class TitleListRefreshes(private val scope: CoroutineScope) {
    /** Refreshes in progress. Guarded by [mutex]. */
    private val running = mutableMapOf<TitleListKey, Deferred<AppResult<Unit>>>()
    private val mutex = Mutex()

    /** Awaits the refresh running for [key]; otherwise launches [refresh] if [needed], else returns `Success`. */
    suspend fun joinOrLaunch(
        key: TitleListKey,
        needed: suspend () -> Boolean,
        refresh: suspend () -> AppResult<Unit>,
    ): AppResult<Unit> {
        val job = mutex.withLock {
            running[key] ?: if (needed()) launch(key, refresh) else null
        }
        return job?.await() ?: AppResult.Success(Unit)
    }

    /** Call with [mutex] held. */
    private fun launch(key: TitleListKey, refresh: suspend () -> AppResult<Unit>): Deferred<AppResult<Unit>> =
        scope.async {
            try {
                refresh()
            } finally {
                mutex.withLock { running -= key }
            }
        }.also { running[key] = it }
}
