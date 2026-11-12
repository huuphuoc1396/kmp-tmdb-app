package com.banhmilabs.tmdb.apps.cmp.composeApp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
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

    @Test
    fun onboardingIsRestoredAfterRecreation() = runComposeUiTest {
        val recreate = setRecreatableContent { TmdbTheme { AppNavHost(startRoute = OnboardingRoute) } }

        recreate()

        onNodeWithTag(GET_STARTED_TAG).assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun homeIsRestoredAfterRecreationWithoutOnboardingBehind() = runComposeUiTest {
        lateinit var navController: NavHostController
        val recreate = setRecreatableContent {
            navController = rememberNavController()
            TmdbTheme { AppNavHost(startRoute = OnboardingRoute, navController = navController) }
        }
        onNodeWithTag(GET_STARTED_TAG).performClick()
        waitForIdle()

        recreate()

        onNodeWithText("Home").assertIsDisplayed()
        onNodeWithTag(GET_STARTED_TAG).assertDoesNotExist()
        assertNull(navController.previousBackStackEntry)
    }

    private fun ComposeUiTest.setRecreatableContent(content: @Composable () -> Unit): () -> Unit {
        var savedState: Map<String, List<Any?>>? = null
        var registry: SaveableStateRegistry? = null
        var shown by mutableStateOf(true)
        setContent {
            if (shown) {
                val currentRegistry = remember { SaveableStateRegistry(savedState) { true } }
                val viewModelStoreOwner = remember {
                    object : ViewModelStoreOwner {
                        override val viewModelStore = ViewModelStore()
                    }
                }
                registry = currentRegistry
                CompositionLocalProvider(
                    LocalSaveableStateRegistry provides currentRegistry,
                    LocalViewModelStoreOwner provides viewModelStoreOwner,
                ) { content() }
            }
        }
        return {
            savedState = checkNotNull(registry).performSave()
            shown = false
            waitForIdle()
            shown = true
            waitForIdle()
        }
    }

    private companion object {
        const val GET_STARTED_TAG = "onboarding_get_started"
    }
}
