package com.banhmilabs.tmdb.nativeapp.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Purple = Color(0xFF8000FF)
private val DeepPurple = Color(0xFF540BA1)
private val Night = Color(0xFF15151D)

// From the home design: selected tab and retry pill; search field, avatar and poster placeholder.
private val Pink = Color(0xFFFF1F8A)
private val Plum = Color(0xFF36076B)

private val TmdbDarkColors = darkColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    primaryContainer = DeepPurple,
    onPrimaryContainer = Color.White,
    secondary = Pink,
    onSecondary = Color.White,
    surfaceVariant = Plum,
    onSurfaceVariant = Color.White,
    background = Night,
    onBackground = Color.White,
    surface = Night,
    onSurface = Color.White,
)

@Composable
fun TmdbTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TmdbDarkColors, content = content)
}
