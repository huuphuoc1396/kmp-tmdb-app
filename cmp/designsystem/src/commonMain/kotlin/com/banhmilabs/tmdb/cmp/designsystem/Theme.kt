package com.banhmilabs.tmdb.cmp.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Purple = Color(0xFF8000FF)
private val DeepPurple = Color(0xFF540BA1)
private val Night = Color(0xFF15151D)
private val Pink = Color(0xFFFF1F8A)
private val Plum = Color(0xFF36076B)

private val TmdbDarkColors = darkColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    primaryContainer = DeepPurple,
    onPrimaryContainer = Color.White,
    secondary = Pink,
    onSecondary = Color.White,
    background = Night,
    onBackground = Color.White,
    surface = Night,
    onSurface = Color.White,
    surfaceVariant = Plum,
    onSurfaceVariant = Color.White,
)

@Composable
fun TmdbTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TmdbDarkColors, content = content)
}
