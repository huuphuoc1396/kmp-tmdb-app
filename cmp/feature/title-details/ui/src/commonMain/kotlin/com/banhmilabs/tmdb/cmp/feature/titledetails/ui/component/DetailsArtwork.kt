package com.banhmilabs.tmdb.cmp.feature.titledetails.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

private const val FADE_START = 260f / 492f
private const val FADE_END = 412f / 492f

@Composable
internal fun DetailsArtwork(url: String?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val fade = remember(colors.surface) {
        Brush.verticalGradient(
            0f to Color.Transparent,
            FADE_START to Color.Transparent,
            FADE_END to colors.surface,
            1f to colors.surface,
        )
    }
    Box(modifier = modifier.fillMaxWidth().height(492.dp).background(colors.surfaceContainerHigh)) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(modifier = Modifier.fillMaxSize().background(fade))
    }
}
