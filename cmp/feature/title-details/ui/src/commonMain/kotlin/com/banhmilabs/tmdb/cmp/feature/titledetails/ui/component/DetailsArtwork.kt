package com.banhmilabs.tmdb.cmp.feature.titledetails.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.DetailsColors

internal val ArtworkHeight = 493.dp

/** Where the design's fade starts and becomes solid, as fractions of the artwork's 493dp. */
private const val FADE_START = 260f / 493f
private const val FADE_END = 412f / 493f

private val Fade = Brush.verticalGradient(
    0f to Color.Transparent,
    FADE_START to Color.Transparent,
    FADE_END to DetailsColors.Page,
    1f to DetailsColors.Page,
)

/** The decorative artwork at the top, fading into the page; a plain tile when there is no image. */
@Composable
internal fun DetailsArtwork(url: String?, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(ArtworkHeight).background(DetailsColors.Track)) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Box(modifier = Modifier.fillMaxSize().background(Fade))
    }
}
