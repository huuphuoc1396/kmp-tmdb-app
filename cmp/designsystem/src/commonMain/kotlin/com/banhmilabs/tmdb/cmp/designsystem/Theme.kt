package com.banhmilabs.tmdb.cmp.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun TmdbTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TmdbDarkColorScheme,
        typography = TmdbTypography,
        shapes = TmdbShapes,
        content = content,
    )
}
