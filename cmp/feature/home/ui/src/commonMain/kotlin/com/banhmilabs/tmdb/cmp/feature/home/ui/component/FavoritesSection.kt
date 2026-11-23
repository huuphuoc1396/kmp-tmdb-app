package com.banhmilabs.tmdb.cmp.feature.home.ui.component

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banhmilabs.tmdb.cmp.designsystem.component.PosterTile
import com.banhmilabs.tmdb.cmp.designsystem.component.removeFavoriteLabel
import com.banhmilabs.tmdb.cmp.designsystem.text.metaLine
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.Res
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_favorites_loading
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_favorites_subtitle
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_favorites_title
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.home_favorites_view_all
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.ic_chevron_right
import com.banhmilabs.tmdb.cmp.feature.home.ui.generated.resources.ic_favorite_remove
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.service.title.domain.FavoriteTitle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val CardColor = Color(0xFF211A30)
private val CardBorderColor = Color(0xFF312A3F)
private val BadgeColor = Color(0xFF381A39)
private val SecondaryText = Color(0xFFA6A1B5)

private val CardShape = RoundedCornerShape(16.dp)
private val CardPadding = 12.dp
private val HeaderTop = 18.dp
private val HeaderEnd = 4.dp
private val TitleToBadge = 8.dp
private val BadgeSize = 22.dp
private val BadgePadding = 6.dp
private val ViewAllPadding = 8.dp
private val ViewAllToChevron = 6.dp
private val ChevronSize = 12.dp
private val TitleToSubtitle = 12.dp
private val SubtitleToRow = 16.dp
private val ItemSpacing = 12.dp
private val ItemWidth = 97.dp
private val PosterHeight = 147.dp
private val PosterShape = RoundedCornerShape(8.dp)
private val PosterToName = 12.dp
private val NameToMeta = 8.dp
private val BottomPadding = 14.dp
private val HeartSize = 28.dp
private val HeartTouchOffset = 4.dp
private const val NAME_MAX_LINES = 2

private val TitleStyle = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
private val BadgeStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold)
private val ViewAllStyle = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
private val SubtitleStyle = TextStyle(color = SecondaryText, fontSize = 13.sp)
private val NameStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
private val MetaStyle = TextStyle(color = SecondaryText, fontSize = 12.sp)

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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(CardColor)
            .border(width = 1.dp, color = CardBorderColor, shape = CardShape)
            .padding(top = HeaderTop, bottom = BottomPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = CardPadding, end = HeaderEnd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.home_favorites_title),
                modifier = Modifier.semantics { heading() },
                style = TitleStyle,
            )
            Spacer(modifier = Modifier.width(TitleToBadge))
            CountBadge(count = count)
            Spacer(modifier = Modifier.weight(1f))
            ViewAll(onClick = onSeeAll)
        }
        Spacer(modifier = Modifier.height(TitleToSubtitle))
        Text(
            text = stringResource(Res.string.home_favorites_subtitle),
            modifier = Modifier.padding(horizontal = CardPadding),
            style = SubtitleStyle,
        )
        Spacer(modifier = Modifier.height(SubtitleToRow))
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = CardPadding),
            horizontalArrangement = Arrangement.spacedBy(ItemSpacing),
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
private fun CountBadge(count: Int) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = BadgeSize, minHeight = BadgeSize)
            .clip(CircleShape)
            .background(BadgeColor)
            .padding(horizontal = BadgePadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = count.toString(), style = BadgeStyle.copy(color = MaterialTheme.colorScheme.secondary))
    }
}

@Composable
private fun ViewAll(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = ViewAllPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.home_favorites_view_all),
            style = ViewAllStyle.copy(color = MaterialTheme.colorScheme.secondary),
        )
        Spacer(modifier = Modifier.width(ViewAllToChevron))
        Image(
            painter = painterResource(Res.drawable.ic_chevron_right),
            contentDescription = null,
            modifier = Modifier.size(ChevronSize),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.secondary),
        )
    }
}

@Composable
private fun FavoriteItem(favorite: FavoriteTitle, onClick: () -> Unit, onRemoveClick: () -> Unit) {
    val summary = favorite.summary
    val label = summary?.name ?: stringResource(Res.string.home_favorites_loading)
    Box(modifier = Modifier.width(ItemWidth)) {
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
                modifier = Modifier.fillMaxWidth().height(PosterHeight).clip(PosterShape),
            )
            if (summary != null) {
                Spacer(modifier = Modifier.height(PosterToName))
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
        RemoveButton(
            onClick = onRemoveClick,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = HeartTouchOffset, y = -HeartTouchOffset),
        )
    }
}

@Composable
private fun RemoveButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = removeFavoriteLabel()
    Image(
        painter = painterResource(Res.drawable.ic_favorite_remove),
        contentDescription = null,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(HeartSize)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
    )
}
