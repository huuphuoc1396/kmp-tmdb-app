package com.banhmilabs.tmdb.cmp.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.banhmilabs.tmdb.cmp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.cmp.designsystem.preview.TmdbComponentPreviews
import com.banhmilabs.tmdb.cmp.designsystem.preview.TmdbPreview
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType

@Composable
fun PosterRow(
    titles: List<Title>,
    onTitleClick: (Title) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    LazyRow(
        modifier = modifier.height(PosterRowDefaults.PosterHeight),
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
            .size(width = PosterRowDefaults.PosterWidth, height = PosterRowDefaults.PosterHeight)
            .clip(MaterialTheme.shapes.small)
            .semantics(mergeDescendants = true) { contentDescription = title.name }
            .clickable(role = Role.Button, onClick = onClick),
    )
}

object PosterRowDefaults {
    val PosterWidth = 124.dp
    val PosterHeight = 188.dp
}

private val previewTitles = listOf("Dune: Part Two", "The Wild Robot", "Kingdom of the Planet of the Apes", "Shōgun")
    .mapIndexed { index, name -> Title(TitleId(index + 1L, TitleType.MOVIE), name, posterUrl = null) }

@TmdbComponentPreviews
@Composable
private fun PosterRowPreview() {
    TmdbPreview {
        PosterRow(
            titles = previewTitles,
            onTitleClick = {},
            modifier = Modifier.padding(vertical = TmdbSpacing.md),
            contentPadding = PaddingValues(horizontal = TmdbSpacing.md),
        )
    }
}
