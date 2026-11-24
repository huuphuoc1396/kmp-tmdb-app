package com.banhmilabs.tmdb.nativeapp.designsystem

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

private val MaterialTypography = Typography()

private fun TextStyle.bold() = copy(fontWeight = FontWeight.Bold)

private fun TextStyle.semiBold() = copy(fontWeight = FontWeight.SemiBold)

internal val TmdbTypography = with(MaterialTypography) {
    copy(
        displayLarge = displayLarge.bold(),
        displayMedium = displayMedium.bold(),
        displaySmall = displaySmall.bold(),
        headlineLarge = headlineLarge.bold(),
        headlineMedium = headlineMedium.bold(),
        headlineSmall = headlineSmall.bold(),
        titleLarge = titleLarge.bold(),
        titleMedium = titleMedium.semiBold(),
        titleSmall = titleSmall.semiBold(),
        labelLarge = labelLarge.semiBold(),
        labelMedium = labelMedium.semiBold(),
        labelSmall = labelSmall.semiBold(),
    )
}
