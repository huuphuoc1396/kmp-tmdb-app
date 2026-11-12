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
class OnboardingContentTest {
    private val defaultLocale = Locale.getDefault()

    @AfterTest
    fun restoreLocale() {
        Locale.setDefault(defaultLocale)
    }

    @Test
    fun showsLogoHeadlineBodyAndButtonInEnglish() = runComposeUiTest {
        Locale.setDefault(Locale.US)
        setContent { TmdbTheme { OnboardingContent(state = OnboardingUiState(), onIntent = {}) } }

        onNodeWithTag(LOGO_TAG).assertIsDisplayed()
        onNodeWithText("All about movies, series, anime and much more.").assertIsDisplayed()
        onNodeWithText("Stay up to date on movies, series, anime and much more.").assertIsDisplayed()
        onNodeWithText("Get started").assertIsDisplayed()
    }

    @Test
    fun showsPortugueseCopyForPtBr() = runComposeUiTest {
        Locale.setDefault(Locale.of("pt", "BR"))
        setContent { TmdbTheme { OnboardingContent(state = OnboardingUiState(), onIntent = {}) } }

        onNodeWithText("Tudo sobre filmes, séries, animes e muito mais.").assertIsDisplayed()
        onNodeWithText("Fique por dentro das informações de filmes, séries, animes e muito mais.").assertIsDisplayed()
        onNodeWithText("Acessar").assertIsDisplayed()
    }

    @Test
    fun fallsBackToEnglishForOtherLanguages() = runComposeUiTest {
        Locale.setDefault(Locale.FRANCE)
        setContent { TmdbTheme { OnboardingContent(state = OnboardingUiState(), onIntent = {}) } }

        onNodeWithText("Get started").assertIsDisplayed()
    }

    @Test
    fun clickSendsGetStartedIntent() = runComposeUiTest {
        val intents = mutableListOf<OnboardingIntent>()
        setContent { TmdbTheme { OnboardingContent(state = OnboardingUiState(), onIntent = { intents += it }) } }

        onNodeWithTag(GET_STARTED_TAG).performClick()

        assertEquals(listOf<OnboardingIntent>(OnboardingIntent.GetStartedClicked), intents)
    }

    @Test
    fun clickIgnoredWhileNavigating() = runComposeUiTest {
        val intents = mutableListOf<OnboardingIntent>()
        val navigating = OnboardingUiState(isNavigating = true)
        setContent { TmdbTheme { OnboardingContent(state = navigating, onIntent = { intents += it }) } }

        onNodeWithTag(GET_STARTED_TAG).performClick()

        assertEquals(emptyList(), intents)
    }
}
