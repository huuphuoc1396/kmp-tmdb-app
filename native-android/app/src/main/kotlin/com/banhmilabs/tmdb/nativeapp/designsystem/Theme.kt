package com.banhmilabs.tmdb.nativeapp.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

@Composable
fun TmdbTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TmdbDarkColors, content = content)
}
