package com.banhmilabs.tmdb.apps.cmp.composeApp

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.banhmilabs.tmdb.apps.cmp.designsystem.TmdbTheme
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.di.onboardingUiModule
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.navigation.OnboardingRoute
import com.banhmilabs.tmdb.core.testing.MainDispatcherTest
import com.banhmilabs.tmdb.service.settings.domain.SettingsRepository
import com.banhmilabs.tmdb.service.settings.fake.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AppNavHostTest : MainDispatcherTest() {
    private val repository = FakeSettingsRepository()

    @BeforeTest
    fun startKoinWithFake() {
        startKoin { modules(module { single<SettingsRepository> { repository } }, onboardingUiModule) }
    }

    @AfterTest
    fun stopKoinAfterTest() {
        stopKoin()
    }

    @Test
    fun onboardingStartShowsOnboarding() = runComposeUiTest {
        setContent { TmdbTheme { AppNavHost(startRoute = OnboardingRoute) } }

        onNodeWithTag(GET_STARTED_TAG).assertIsDisplayed()
    }

    @Test
    fun homeStartShowsHomeWithoutOnboarding() = runComposeUiTest {
        setContent { TmdbTheme { AppNavHost(startRoute = HomeRoute) } }

        onNodeWithText("Home").assertIsDisplayed()
        onNodeWithTag(GET_STARTED_TAG).assertDoesNotExist()
    }

    @Test
    fun getStartedOpensHomeAndRemovesOnboardingFromBackStack() = runComposeUiTest {
        lateinit var navController: NavHostController
        setContent {
            navController = rememberNavController()
            TmdbTheme { AppNavHost(startRoute = OnboardingRoute, navController = navController) }
        }

        onNodeWithTag(GET_STARTED_TAG).performClick()
        waitForIdle()

        onNodeWithText("Home").assertIsDisplayed()
        onNodeWithTag(GET_STARTED_TAG).assertDoesNotExist()
        assertNull(navController.previousBackStackEntry)
        assertTrue(runBlocking { repository.observeOnboardingCompleted().first() })
    }

    private companion object {
        const val GET_STARTED_TAG = "onboarding_get_started"
    }
}
