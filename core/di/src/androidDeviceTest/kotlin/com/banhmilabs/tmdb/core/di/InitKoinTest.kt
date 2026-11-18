package com.banhmilabs.tmdb.core.di

import androidx.test.platform.app.InstrumentationRegistry
import com.banhmilabs.tmdb.service.settings.domain.SettingsRepository
import com.banhmilabs.tmdb.service.title.domain.HomeListsRepository
import org.koin.android.ext.koin.androidContext
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
        val koin = initKoin { androidContext(InstrumentationRegistry.getInstrumentation().targetContext) }.koin

        assertNotNull(koin.getOrNull<SettingsRepository>())
    }

    @Test
    fun initKoinProvidesHomeListsRepository() {
        val koin = initKoin { androidContext(InstrumentationRegistry.getInstrumentation().targetContext) }.koin

        assertNotNull(koin.getOrNull<HomeListsRepository>())
    }
}
