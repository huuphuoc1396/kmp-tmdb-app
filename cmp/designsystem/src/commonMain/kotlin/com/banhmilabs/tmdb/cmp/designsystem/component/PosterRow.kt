package com.banhmilabs.tmdb.cmp.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSize
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.core.model.Title

@Composable
fun PosterRow(
    titles: List<Title>,
    onTitleClick: (Title) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyRow(
        modifier = modifier.height(TmdbSize.posterHeight),
        state = listState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(TmdbSpacing.sm),
    ) {
        items(titles, key = { "${it.id.type}-${it.id.tmdbId}" }) { title ->
            Poster(title = title, onClick = { onTitleClick(title) })
        }
    }
}

@Composable
private fun Poster(title: Title, onClick: () -> Unit) {
    PosterTile(
        name = title.name,
        posterUrl = title.posterUrl,
        modifier = Modifier
            .size(width = TmdbSize.posterWidth, height = TmdbSize.posterHeight)
            .clip(MaterialTheme.shapes.small)
            .semantics(mergeDescendants = true) { contentDescription = title.name }
            .clickable(role = Role.Button, onClick = onClick),
    )
}
