package com.banhmilabs.tmdb.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/** Base class for tests of code that runs on `Dispatchers.Main` (ViewModels): swaps Main for [testDispatcher]. */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class MainDispatcherTest(protected val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()) {
    @BeforeTest
    fun setMainDispatcher() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
    }
}
