package com.banhmilabs.tmdb.core.ios

import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class SharedCoreTest {
    @AfterTest
    fun stop() {
        SharedCore.reset()
        stopKoin()
    }

    @Test
    fun startProvidesSettingsRepository() {
        SharedCore.start()

        assertNotNull(SharedCore.settingsRepository)
    }

    @Test
    fun startProvidesHomeListsRepository() {
        SharedCore.start()

        assertNotNull(SharedCore.homeListsRepository)
    }

    @Test
    fun startTwiceKeepsTheSameCore() {
        SharedCore.start()
        val repository = SharedCore.settingsRepository

        SharedCore.start()

        assertSame(repository, SharedCore.settingsRepository)
    }

    @Test
    fun settingsRepositoryBeforeStartFails() {
        assertFailsWith<IllegalStateException> { SharedCore.settingsRepository }
    }
}
