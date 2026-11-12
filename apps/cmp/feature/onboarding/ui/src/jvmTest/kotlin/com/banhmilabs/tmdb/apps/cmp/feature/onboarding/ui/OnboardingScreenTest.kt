package com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.banhmilabs.tmdb.apps.cmp.designsystem.TmdbTheme
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class OnboardingScreenTest {
    private val defaultLocale = Locale.getDefault()

    @AfterTest
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun showsLogoHeadlineBodyAndButtonInEnglish() = runComposeUiTest {
        Locale.setDefault(Locale.US)
        setContent { TmdbTheme { OnboardingScreen(isNavigating = false, onGetStartedClick = {}) } }

        onNodeWithTag(LOGO_TAG).assertIsDisplayed()
        onNodeWithText("All about movies, series, anime and much more.").assertIsDisplayed()
        onNodeWithText("Stay up to date on movies, series, anime and much more.").assertIsDisplayed()
        onNodeWithText("Get started").assertIsDisplayed()
    }

    @Test
    fun showsPortugueseCopyForPtBr() = runComposeUiTest {
        Locale.setDefault(Locale.of("pt", "BR"))
        setContent { TmdbTheme { OnboardingScreen(isNavigating = false, onGetStartedClick = {}) } }

        onNodeWithText("Tudo sobre filmes, séries, animes e muito mais.").assertIsDisplayed()
        onNodeWithText("Fique por dentro das informações de filmes, séries, animes e muito mais.").assertIsDisplayed()
        onNodeWithText("Acessar").assertIsDisplayed()
    }

    @Test
    fun fallsBackToEnglishForOtherLanguages() = runComposeUiTest {
        Locale.setDefault(Locale.FRANCE)
        setContent { TmdbTheme { OnboardingScreen(isNavigating = false, onGetStartedClick = {}) } }

        onNodeWithText("Get started").assertIsDisplayed()
    }

    @Test
    fun clickCallsOnGetStartedClick() = runComposeUiTest {
        var clicks = 0
        setContent { TmdbTheme { OnboardingScreen(isNavigating = false, onGetStartedClick = { clicks++ }) } }

        onNodeWithTag(GET_STARTED_TAG).performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun clickIgnoredWhileNavigating() = runComposeUiTest {
        var clicks = 0
        setContent { TmdbTheme { OnboardingScreen(isNavigating = true, onGetStartedClick = { clicks++ }) } }

        onNodeWithTag(GET_STARTED_TAG).performClick()

        assertEquals(0, clicks)
    }
}
