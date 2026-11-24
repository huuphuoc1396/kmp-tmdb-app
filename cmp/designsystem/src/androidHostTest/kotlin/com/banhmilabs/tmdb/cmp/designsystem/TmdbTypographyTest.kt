package com.banhmilabs.tmdb.cmp.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals

class TmdbTypographyTest {

    @Test
    fun sizesAndLineHeightsMatchMaterialDefaults() {
        styles(TmdbTypography).zip(styles(Typography())).forEach { (tmdb, material) ->
            assertEquals(material.fontSize, tmdb.fontSize)
            assertEquals(material.lineHeight, tmdb.lineHeight)
            assertEquals(material.letterSpacing, tmdb.letterSpacing)
        }
    }

    @Test
    fun displayHeadlineAndTitleLargeAreBold() {
        with(TmdbTypography) {
            listOf(
                displayLarge,
                displayMedium,
                displaySmall,
                headlineLarge,
                headlineMedium,
                headlineSmall,
                titleLarge,
            ).forEach { assertEquals(FontWeight.Bold, it.fontWeight) }
        }
    }

    @Test
    fun titleMediumTitleSmallAndLabelsAreSemiBold() {
        with(TmdbTypography) {
            listOf(titleMedium, titleSmall, labelLarge, labelMedium, labelSmall)
                .forEach { assertEquals(FontWeight.SemiBold, it.fontWeight) }
        }
    }

    @Test
    fun bodyRolesKeepTheMaterialWeight() {
        val material = Typography()
        assertEquals(material.bodyLarge.fontWeight, TmdbTypography.bodyLarge.fontWeight)
        assertEquals(material.bodyMedium.fontWeight, TmdbTypography.bodyMedium.fontWeight)
        assertEquals(material.bodySmall.fontWeight, TmdbTypography.bodySmall.fontWeight)
    }

    private fun styles(typography: Typography): List<TextStyle> = with(typography) {
        listOf(
            displayLarge, displayMedium, displaySmall,
            headlineLarge, headlineMedium, headlineSmall,
            titleLarge, titleMedium, titleSmall,
            bodyLarge, bodyMedium, bodySmall,
            labelLarge, labelMedium, labelSmall,
        )
    }
}
