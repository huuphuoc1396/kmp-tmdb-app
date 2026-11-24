package com.banhmilabs.tmdb.nativeapp.home.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.nativeapp.R
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbSize
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.nativeapp.designsystem.component.FavoriteHeart
import com.banhmilabs.tmdb.nativeapp.designsystem.component.PosterTile
import com.banhmilabs.tmdb.nativeapp.designsystem.text.NameText
import com.banhmilabs.tmdb.nativeapp.designsystem.text.metaLine
import com.banhmilabs.tmdb.service.title.domain.FavoriteTitle

@Composable
internal fun FavoritesSection(
    favorites: List<FavoriteTitle>,
    count: Int,
    onFavoriteClick: (TitleId) -> Unit,
    onRemoveClick: (FavoriteTitle) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainer)
            .border(width = TmdbSize.borderWidth, color = colors.surfaceContainerHigh, shape = shape)
            .padding(top = TmdbSpacing.lg, bottom = TmdbSpacing.md),
    ) {
        FavoritesHeader(
            count = count,
            onSeeAll = onSeeAll,
            modifier = Modifier.fillMaxWidth().padding(start = TmdbSpacing.sm, end = TmdbSpacing.xxs),
        )
        Spacer(modifier = Modifier.height(TmdbSpacing.sm))
        Text(
            text = stringResource(R.string.home_favorites_subtitle),
            modifier = Modifier.padding(horizontal = TmdbSpacing.sm),
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.height(TmdbSpacing.md))
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = TmdbSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(TmdbSpacing.sm),
        ) {
            items(favorites, key = { "${it.id.type}-${it.id.tmdbId}" }) { favorite ->
                FavoriteItem(
                    favorite = favorite,
                    onClick = { onFavoriteClick(favorite.id) },
                    onRemoveClick = { onRemoveClick(favorite) },
                )
            }
        }
    }
}

@Composable
private fun FavoritesHeader(count: Int, onSeeAll: () -> Unit, modifier: Modifier = Modifier) {
    Layout(
        modifier = modifier,
        content = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_favorites_title),
                    modifier = Modifier.semantics { heading() },
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.width(TmdbSpacing.xs))
                CountBadge(count = count)
            }
            ViewAll(onClick = onSeeAll)
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val title = measurables[0].measure(loose)
        val viewAll = measurables[1].measure(loose)
        val width = constraints.maxWidth
        if (title.width + viewAll.width <= width) {
            val height = maxOf(title.height, viewAll.height)
            layout(width, height) {
                title.placeRelative(0, (height - title.height) / 2)
                viewAll.placeRelative(width - viewAll.width, (height - viewAll.height) / 2)
            }
        } else {
            layout(width, title.height + viewAll.height) {
                title.placeRelative(0, 0)
                viewAll.placeRelative(width - viewAll.width, title.height)
            }
        }
    }
}

@Composable
private fun CountBadge(count: Int) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 24.dp, minHeight = 24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = TmdbSpacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ViewAll(onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.secondary
    Row(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = TmdbSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_favorites_view_all),
            modifier = Modifier.weight(1f, fill = false),
            color = accent,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(TmdbSpacing.xs))
        Image(
            painter = painterResource(R.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            colorFilter = ColorFilter.tint(accent),
        )
    }
}

@Composable
private fun FavoriteItem(favorite: FavoriteTitle, onClick: () -> Unit, onRemoveClick: () -> Unit) {
    val summary = favorite.summary
    val label = summary?.name ?: stringResource(R.string.home_favorites_loading)
    Box(modifier = Modifier.width(96.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .clearAndSetSemantics {
                    contentDescription = label
                    role = Role.Button
                },
        ) {
            PosterTile(
                name = summary?.name,
                posterUrl = summary?.posterUrl,
                modifier = Modifier.fillMaxWidth().height(148.dp).clip(MaterialTheme.shapes.small),
            )
            if (summary != null) {
                Spacer(modifier = Modifier.height(TmdbSpacing.sm))
                NameText(
                    text = summary.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
                val meta = metaLine(summary.releaseYear, summary.firstGenre)
                if (meta.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(TmdbSpacing.xs))
                    Text(
                        text = meta,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        RemoveButton(
            onClick = onRemoveClick,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = TmdbSpacing.xxs, y = -TmdbSpacing.xxs),
        )
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
