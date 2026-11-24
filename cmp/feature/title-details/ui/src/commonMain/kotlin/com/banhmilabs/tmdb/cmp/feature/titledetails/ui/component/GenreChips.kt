package com.banhmilabs.tmdb.cmp.feature.titledetails.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSpacing

@Composable
internal fun GenreChips(genres: List<String>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(TmdbSpacing.md),
        verticalArrangement = Arrangement.spacedBy(TmdbSpacing.sm),
    ) {
        genres.forEach { genre ->
            Box(
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                    .padding(horizontal = TmdbSpacing.md),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = genre,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
