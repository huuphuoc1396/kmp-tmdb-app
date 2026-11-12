package com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.banhmilabs.tmdb.apps.cmp.designsystem.TmdbTheme
import com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui.component.GET_STARTED_TAG
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

    @Test
    fun buttonIsAnnouncedAsButtonWithLabel() = runComposeUiTest {
        Locale.setDefault(Locale.US)
        setContent { TmdbTheme { OnboardingContent(state = OnboardingUiState(), onIntent = {}) } }

        onNodeWithTag(GET_STARTED_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertHasClickAction()
            .assertTextEquals("Get started")
    }

    @Test
    fun buttonMeetsMinimumTouchTarget() = runComposeUiTest {
        setContent { TmdbTheme { OnboardingContent(state = OnboardingUiState(), onIntent = {}) } }

        onNodeWithTag(GET_STARTED_TAG).assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun decorativeImagesAreNotAnnounced() = runComposeUiTest {
        setContent { TmdbTheme { OnboardingContent(state = OnboardingUiState(), onIntent = {}) } }

        onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription), useUnmergedTree = true)
            .assertCountEquals(0)
        onNodeWithTag(LOGO_TAG).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
    }

    @Test
    fun headlineIsAHeading() = runComposeUiTest {
        Locale.setDefault(Locale.US)
        setContent { TmdbTheme { OnboardingContent(state = OnboardingUiState(), onIntent = {}) } }

        onNodeWithText(HEADLINE).assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun largestFontKeepsTextAndButtonReachable() = runComposeUiTest {
        Locale.setDefault(Locale.US)
        setOnboarding(DpSize(360.dp, 640.dp), fontScale = 2f)

        onNodeWithText(HEADLINE).performScrollTo().assertIsDisplayed()
        onNodeWithText(BODY).performScrollTo().assertIsDisplayed()
        onNodeWithTag(GET_STARTED_TAG).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun landscapePhoneKeepsButtonReachable() = runComposeUiTest {
        setOnboarding(DpSize(800.dp, 360.dp))

        onNodeWithTag(GET_STARTED_TAG).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun tabletCentresContentAtMaxWidth() = runComposeUiTest {
        setOnboarding(DpSize(1280.dp, 800.dp))

        val screen = onNodeWithTag(SCREEN_TAG).getUnclippedBoundsInRoot()
        val content = onNodeWithTag(CONTENT_TAG).getUnclippedBoundsInRoot()
        assertEquals(315f, content.width.value, absoluteTolerance = 1f)
        val contentCentre = (content.left + content.right) / 2 - screen.left
        assertEquals(639f, contentCentre.value, absoluteTolerance = 1f)
    }

    @Test
    fun portraitPhoneKeepsDesignWidth() = runComposeUiTest {
        setOnboarding(DpSize(375.dp, 812.dp))

        assertEquals(315f, onNodeWithTag(CONTENT_TAG).getUnclippedBoundsInRoot().width.value, absoluteTolerance = 1f)
    }

    private fun ComposeUiTest.setOnboarding(screen: DpSize, fontScale: Float = 1f) {
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                Box(modifier = Modifier.requiredSize(screen).testTag(SCREEN_TAG)) {
                    TmdbTheme { OnboardingContent(state = OnboardingUiState(), onIntent = {}) }
                }
            }
        }
    }

    private companion object {
        const val HEADLINE = "All about movies, series, anime and much more."
        const val SCREEN_TAG = "screen"
        const val BODY = "Stay up to date on movies, series, anime and much more."
    }
}
