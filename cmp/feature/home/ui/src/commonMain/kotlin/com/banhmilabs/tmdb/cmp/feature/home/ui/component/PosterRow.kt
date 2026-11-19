package com.banhmilabs.tmdb.cmp.feature.home.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.banhmilabs.tmdb.cmp.feature.home.ui.ScreenPadding
import com.banhmilabs.tmdb.core.model.Title

private val PosterWidth = 124.dp
private val PosterHeight = 188.dp
private val PosterShape = RoundedCornerShape(8.dp)
private val PosterSpacing = 12.dp
private val PlaceholderPadding = 8.dp
private const val PLACEHOLDER_MAX_LINES = 4

private val PlaceholderNameStyle = TextStyle(
    color = Color.White,
    fontSize = 14.sp,
    fontWeight = FontWeight.SemiBold,
    textAlign = TextAlign.Center,
)

/** A row of posters that scrolls sideways and bleeds to the end edge. */
@Composable
internal fun PosterRow(titles: List<Title>, onTitleClick: (Title) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(
        modifier = modifier.height(PosterHeight),
        contentPadding = PaddingValues(start = ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(PosterSpacing),
    ) {
        // Lazy list keys must be saveable on Android, so the id becomes a string.
        items(titles, key = { "${it.id.type}-${it.id.tmdbId}" }) { title ->
            Poster(title = title, onClick = { onTitleClick(title) })
        }
    }
}

/** The poster image, or a tile with the title's name when TMDB has no poster. */
@Composable
private fun Poster(title: Title, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = PosterWidth, height = PosterHeight)
            .clip(PosterShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val posterUrl = title.posterUrl
        if (posterUrl == null) {
            Text(
                text = title.name,
                modifier = Modifier.padding(PlaceholderPadding),
                style = PlaceholderNameStyle,
                maxLines = PLACEHOLDER_MAX_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            AsyncImage(
                model = posterUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
