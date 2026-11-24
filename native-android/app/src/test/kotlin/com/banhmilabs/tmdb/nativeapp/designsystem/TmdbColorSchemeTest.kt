package com.banhmilabs.tmdb.nativeapp.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TmdbColorSchemeTest {

    private val scheme = TmdbDarkColorScheme

    @Test
    fun primaryIsTheBrandPurple() {
        assertEquals(Color(0xFF8000FF), scheme.primary)
    }

    @Test
    fun secondaryIsTheBrandPink() {
        assertEquals(Color(0xFFFF1F8A), scheme.secondary)
    }

    @Test
    fun surfacesUseTheFavoritesPurpleFamily() {
        assertEquals(Color(0xFF130E1F), scheme.surface)
        assertEquals(Color(0xFF130E1F), scheme.background)
        assertEquals(Color(0xFF211A30), scheme.surfaceContainer)
        assertEquals(Color(0xFF312A3F), scheme.surfaceContainerHigh)
        assertEquals(Color(0xFFA6A1B5), scheme.onSurfaceVariant)
        assertEquals(Color(0xFF655F74), scheme.outline)
    }

    @Test
    fun labelsOnTheBrandPinkReachThreeToOne() {
        assertContrastAtLeast(MIN_LARGE_TEXT_CONTRAST, scheme.onSecondary, scheme.secondary)
    }

    @Test
    fun secondaryTextOnSurfaceContainersReachesAa() {
        assertContrastAtLeast(MIN_TEXT_CONTRAST, scheme.secondary, scheme.surfaceContainer)
    }

    @Test
    fun textRolesReachAaOnTheirBackgrounds() {
        listOf(
            scheme.onPrimary to scheme.primary,
            scheme.onPrimaryContainer to scheme.primaryContainer,
            scheme.onSecondaryContainer to scheme.secondaryContainer,
            scheme.onSurface to scheme.surface,
            scheme.onSurface to scheme.surfaceContainer,
            scheme.onSurface to scheme.surfaceVariant,
            scheme.onSurfaceVariant to scheme.surface,
            scheme.onSurfaceVariant to scheme.surfaceContainer,
        ).forEach { (text, background) -> assertContrastAtLeast(MIN_TEXT_CONTRAST, text, background) }
    }

    private fun assertContrastAtLeast(minimum: Float, text: Color, background: Color) {
        val ratio = contrast(text, background)
        assertTrue(ratio >= minimum, "$text on $background is $ratio:1, below $minimum:1")
    }

    private fun contrast(a: Color, b: Color): Float {
        val lighter = maxOf(a.luminance(), b.luminance())
        val darker = minOf(a.luminance(), b.luminance())
        return (lighter + LUMINANCE_OFFSET) / (darker + LUMINANCE_OFFSET)
    }

    private companion object {
        const val MIN_TEXT_CONTRAST = 4.5f
        const val MIN_LARGE_TEXT_CONTRAST = 3f
        const val LUMINANCE_OFFSET = 0.05f
    }
}
