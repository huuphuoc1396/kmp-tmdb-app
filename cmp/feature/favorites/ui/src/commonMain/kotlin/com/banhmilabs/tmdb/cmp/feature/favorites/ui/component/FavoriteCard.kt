package com.banhmilabs.tmdb.cmp.feature.favorites.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSize
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.cmp.designsystem.component.PosterTile
import com.banhmilabs.tmdb.cmp.designsystem.component.removeFavoriteLabel
import com.banhmilabs.tmdb.cmp.designsystem.text.metaLine
import com.banhmilabs.tmdb.cmp.feature.favorites.ui.generated.resources.Res
import com.banhmilabs.tmdb.cmp.feature.favorites.ui.generated.resources.favorites_loading
import com.banhmilabs.tmdb.cmp.feature.favorites.ui.generated.resources.ic_heart_filled
import com.banhmilabs.tmdb.service.title.domain.FavoriteTitle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private const val NAME_MAX_LINES = 2

@Composable
internal fun FavoriteCard(
    favorite: FavoriteTitle,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val summary = favorite.summary
    val label = summary?.name ?: stringResource(Res.string.favorites_loading)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = TmdbSize.favoriteCardMinHeight)
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
                    .size(width = TmdbSize.posterSmallWidth, height = TmdbSize.posterSmallHeight)
                    .clip(MaterialTheme.shapes.small),
            )
            Spacer(modifier = Modifier.width(TmdbSpacing.md))
            if (summary != null) {
                Column {
                    Text(
                        text = summary.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = NAME_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
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
    val label = removeFavoriteLabel()
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_heart_filled),
            contentDescription = null,
            modifier = Modifier.size(TmdbSize.iconSmall),
        )
    }
}
