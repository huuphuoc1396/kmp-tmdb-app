package com.banhmilabs.tmdb.core.di

import com.banhmilabs.tmdb.service.settings.domain.SettingsRepository
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class InitKoinTest {
    @AfterTest
    fun stop() {
        stopKoin()
    }

    @Test
    fun initKoinProvidesSettingsRepository() {
        val koin = initKoin().koin

        assertNotNull(koin.getOrNull<SettingsRepository>())
    }
}
