package com.banhmilabs.tmdb.nativeapp.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.banhmilabs.tmdb.core.model.Title

private val PosterWidth = 124.dp
private val PosterHeight = 188.dp
private val PosterShape = RoundedCornerShape(8.dp)
private val PosterSpacing = 12.dp

@Composable
internal fun PosterRow(
    titles: List<Title>,
    onTitleClick: (Title) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyRow(
        modifier = modifier.height(PosterHeight),
        state = listState,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(PosterSpacing),
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
            .size(width = PosterWidth, height = PosterHeight)
            .clip(PosterShape)
            .semantics(mergeDescendants = true) { contentDescription = title.name }
            .clickable(role = Role.Button, onClick = onClick),
    )
}
