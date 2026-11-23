package com.banhmilabs.tmdb.nativeapp.favorites.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banhmilabs.tmdb.nativeapp.R
import com.banhmilabs.tmdb.nativeapp.designsystem.component.PosterTile
import com.banhmilabs.tmdb.nativeapp.designsystem.text.metaLine
import com.banhmilabs.tmdb.nativeapp.favorites.FavoritesColors
import com.banhmilabs.tmdb.service.title.domain.FavoriteTitle

private val CardHeight = 120.dp
private val CardShape = RoundedCornerShape(12.dp)
private val CardPadding = 12.dp
private val PosterWidth = 64.dp
private val PosterHeight = 96.dp
private val PosterShape = RoundedCornerShape(6.dp)
private val PosterToText = 14.dp
private val NameToMeta = 8.dp
private val HeartWidth = 20.dp
private val HeartHeight = 18.dp
private val HeartEndPadding = 10.dp
private const val NAME_MAX_LINES = 2

private val NameStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
private val MetaStyle = TextStyle(color = FavoritesColors.SecondaryText, fontSize = 13.sp)

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
            .height(CardHeight)
            .clip(CardShape)
            .background(FavoritesColors.Card)
            .padding(start = CardPadding),
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
                .padding(vertical = CardPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PosterTile(
                name = summary?.name,
                posterUrl = summary?.posterUrl,
                modifier = Modifier.size(width = PosterWidth, height = PosterHeight).clip(PosterShape),
            )
            Spacer(modifier = Modifier.width(PosterToText))
            if (summary != null) {
                Column {
                    Text(
                        text = summary.name,
                        style = NameStyle,
                        maxLines = NAME_MAX_LINES,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val meta = metaLine(summary.releaseYear, summary.firstGenre)
                    if (meta.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(NameToMeta))
                        Text(text = meta, style = MetaStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        RemoveButton(onClick = onRemoveClick, modifier = Modifier.padding(horizontal = HeartEndPadding))
    }
}

@Composable
private fun RemoveButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(R.string.remove_favorite_heart)
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_heart_filled),
            contentDescription = null,
            modifier = Modifier.size(width = HeartWidth, height = HeartHeight),
        )
    }
}
