package com.banhmilabs.tmdb.service.title.data

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TitleListRefreshesTest {
    private var refreshCount = 0

    @Test
    fun notNeededReturnsSuccessWithoutRefreshing() = runTest {
        val result = TitleListRefreshes(backgroundScope).joinOrLaunch(KEY, needed = { false }) { count() }

        assertEquals(AppResult.Success(Unit), result)
        assertEquals(0, refreshCount)
    }

    @Test
    fun neededLaunchesRefreshAndReturnsItsResult() = runTest {
        val result = TitleListRefreshes(backgroundScope).joinOrLaunch(KEY, needed = { true }) {
            count()
            AppResult.Failure(AppError.Network)
        }

        assertEquals(AppResult.Failure(AppError.Network), result)
        assertEquals(1, refreshCount)
    }

    @Test
    fun secondCallJoinsRunningRefresh() = runTest {
        val refreshes = TitleListRefreshes(backgroundScope)
        val gate = CompletableDeferred<Unit>()
        val slowRefresh: suspend () -> AppResult<Unit> = {
            gate.await()
            count()
        }

        val first = async { refreshes.joinOrLaunch(KEY, needed = { true }, refresh = slowRefresh) }
        testScheduler.runCurrent()
        val second = async { refreshes.joinOrLaunch(KEY, needed = { true }, refresh = slowRefresh) }
        testScheduler.runCurrent()
        gate.complete(Unit)

        assertEquals(AppResult.Success(Unit), first.await())
        assertEquals(AppResult.Success(Unit), second.await())
        assertEquals(1, refreshCount)
    }

    @Test
    fun finishedRefreshIsLaunchedAgain() = runTest {
        val refreshes = TitleListRefreshes(backgroundScope)

        refreshes.joinOrLaunch(KEY, needed = { true }) { count() }
        refreshes.joinOrLaunch(KEY, needed = { true }) { count() }

        assertEquals(2, refreshCount)
    }

    @Test
    fun differentListsRefreshSeparately() = runTest {
        val refreshes = TitleListRefreshes(backgroundScope)

        refreshes.joinOrLaunch(KEY, needed = { true }) { count() }
        refreshes.joinOrLaunch(OTHER, needed = { true }) { count() }

        assertEquals(2, refreshCount)
    }

    private fun count(): AppResult<Unit> {
        refreshCount++
        return AppResult.Success(Unit)
    }

    private companion object {
        val KEY = TitleListKey(TitleCategory.MOVIES, TitleListType.POPULAR, "en-US")
        val OTHER = TitleListKey(TitleCategory.MOVIES, TitleListType.POPULAR, "pt-BR")
    }
}
