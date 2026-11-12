package com.banhmilabs.tmdb.apps.cmp.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colors from docs/features/onboarding/design/onboarding.md.
private val Purple = Color(0xFF8000FF)
private val DeepPurple = Color(0xFF540BA1)
private val Night = Color(0xFF15151D)

private val TmdbDarkColors = darkColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    primaryContainer = DeepPurple,
    onPrimaryContainer = Color.White,
    background = Night,
    onBackground = Color.White,
    surface = Night,
    onSurface = Color.White,
)

/** The app's dark theme with purple accents. */
@Composable
fun TmdbTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TmdbDarkColors, content = content)
}
