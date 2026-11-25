package com.banhmilabs.tmdb.nativeapp.favorites.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.banhmilabs.tmdb.nativeapp.R
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.nativeapp.designsystem.component.FavoriteHeart
import com.banhmilabs.tmdb.nativeapp.designsystem.component.PosterTile
import com.banhmilabs.tmdb.nativeapp.designsystem.preview.TmdbComponentPreviews
import com.banhmilabs.tmdb.nativeapp.designsystem.preview.TmdbPreview
import com.banhmilabs.tmdb.nativeapp.designsystem.text.NameText
import com.banhmilabs.tmdb.nativeapp.designsystem.text.metaLine
import com.banhmilabs.tmdb.nativeapp.favorites.previewFavorites
import com.banhmilabs.tmdb.service.title.domain.FavoriteTitle

@Composable
internal fun FavoriteCard(
    favorite: FavoriteTitle,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = favorite.summary
    val label = summary?.name ?: stringResource(R.string.favorites_loading)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .height(IntrinsicSize.Min)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = TmdbSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(role = Role.Button, onClick = onClick)
                .clearAndSetSemantics {
                    contentDescription = label
                    role = Role.Button
                }
                .padding(vertical = TmdbSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PosterTile(
                name = summary?.name,
                posterUrl = summary?.posterUrl,
                modifier = Modifier
                    .size(width = 64.dp, height = 96.dp)
                    .clip(MaterialTheme.shapes.small),
            )
            Spacer(modifier = Modifier.width(TmdbSpacing.md))
            if (summary != null) {
                Column {
                    NameText(
                        text = summary.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                    )
                    val meta = metaLine(summary.releaseYear, summary.firstGenre)
                    if (meta.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(TmdbSpacing.xs))
                        Text(
                            text = meta,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
        RemoveButton(onClick = onRemoveClick, modifier = Modifier.padding(horizontal = TmdbSpacing.sm))
    }
}

@Composable
private fun RemoveButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.remove_favorite_heart)
    FavoriteHeart(
        isFavorite = true,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
    )
}

@TmdbComponentPreviews
@Composable
private fun FavoriteCardPreview() {
    TmdbPreview {
        Column(
            modifier = Modifier.padding(TmdbSpacing.md),
            verticalArrangement = Arrangement.spacedBy(TmdbSpacing.md),
        ) {
            previewFavorites.forEach { favorite ->
                FavoriteCard(
                    favorite = favorite,
                    onClick = {},
                    onRemoveClick = {},
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
