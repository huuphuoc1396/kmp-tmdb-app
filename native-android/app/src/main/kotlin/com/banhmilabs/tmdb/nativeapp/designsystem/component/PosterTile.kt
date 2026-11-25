package com.banhmilabs.tmdb.nativeapp.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import coil3.compose.AsyncImage
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.nativeapp.designsystem.preview.TmdbComponentPreviews
import com.banhmilabs.tmdb.nativeapp.designsystem.preview.TmdbPreview
import com.banhmilabs.tmdb.nativeapp.designsystem.text.NameText

@Composable
internal fun PosterTile(name: String?, posterUrl: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (posterUrl != null) {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (name != null) {
            NameText(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(TmdbSpacing.xs).clearAndSetSemantics {},
                maxLines = 4,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@TmdbComponentPreviews
@Composable
private fun PosterTilePreview() {
    TmdbPreview {
        Row(
            modifier = Modifier.padding(TmdbSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(TmdbSpacing.md),
        ) {
            PosterTile(
                name = "Kingdom of the Planet of the Apes",
                posterUrl = null,
                modifier = Modifier.width(PosterRowDefaults.PosterWidth),
            )
            PosterTile(name = null, posterUrl = null, modifier = Modifier.width(PosterRowDefaults.PosterWidth))
        }
    }
}
