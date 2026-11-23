package com.banhmilabs.tmdb.cmp.feature.favorites.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banhmilabs.tmdb.cmp.designsystem.component.PosterTile
import com.banhmilabs.tmdb.cmp.designsystem.text.metaLine
import com.banhmilabs.tmdb.cmp.feature.favorites.ui.FavoritesColors
import com.banhmilabs.tmdb.cmp.feature.favorites.ui.generated.resources.Res
import com.banhmilabs.tmdb.cmp.feature.favorites.ui.generated.resources.favorites_loading
import com.banhmilabs.tmdb.service.title.domain.FavoriteTitle
import org.jetbrains.compose.resources.stringResource

private val CardHeight = 120.dp
private val CardShape = RoundedCornerShape(12.dp)
private val CardPadding = 12.dp
private val PosterWidth = 64.dp
private val PosterHeight = 96.dp
private val PosterShape = RoundedCornerShape(6.dp)
private val PosterToText = 14.dp
private val NameToMeta = 8.dp
private const val NAME_MAX_LINES = 2

private val NameStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
private val MetaStyle = TextStyle(color = FavoritesColors.SecondaryText, fontSize = 13.sp)

@Composable
internal fun FavoriteCard(favorite: FavoriteTitle, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val summary = favorite.summary
    val label = summary?.name ?: stringResource(Res.string.favorites_loading)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(CardHeight)
            .clip(CardShape)
            .background(FavoritesColors.Card)
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
            }
            .padding(CardPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PosterTile(
            name = summary?.name,
            posterUrl = summary?.posterUrl,
            modifier = Modifier.size(width = PosterWidth, height = PosterHeight).clip(PosterShape),
        )
        Spacer(modifier = Modifier.width(PosterToText))
        if (summary != null) {
            Column(modifier = Modifier.weight(1f)) {
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
}
