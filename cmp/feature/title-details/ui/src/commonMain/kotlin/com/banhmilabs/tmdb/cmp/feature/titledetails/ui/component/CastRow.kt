package com.banhmilabs.tmdb.cmp.feature.titledetails.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.banhmilabs.tmdb.cmp.designsystem.component.PersonIcon
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.DetailsColors
import com.banhmilabs.tmdb.service.title.domain.CastMember

private val ItemWidth = 75.dp
private val ItemSpacing = 4.dp
private val PhotoSize = 60.dp
private val BorderWidth = 4.dp
private val PhotoToName = 3.dp
private val PlaceholderIconSize = 30.dp
private const val NAME_MAX_LINES = 2

private val NameStyle = TextStyle(
    color = Color.White,
    fontSize = 12.sp,
    fontWeight = FontWeight.Bold,
    textAlign = TextAlign.Center,
)

@Composable
internal fun CastRow(
    cast: List<CastMember>,
    startPadding: Dp,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    LazyRow(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(start = startPadding),
        horizontalArrangement = Arrangement.spacedBy(ItemSpacing),
    ) {
        itemsIndexed(cast, key = { index, member -> "$index-${member.name}" }) { _, member ->
            CastItem(member)
        }
    }
}

@Composable
private fun CastItem(member: CastMember) {
    Column(
        modifier = Modifier.width(ItemWidth).semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(PhotoSize + BorderWidth * 2)
                .border(BorderWidth, DetailsColors.Track, CircleShape)
                .padding(BorderWidth)
                .clip(CircleShape)
                .background(DetailsColors.Track),
            contentAlignment = Alignment.Center,
        ) {
            val photoUrl = member.photoUrl
            if (photoUrl == null) {
                PersonIcon(modifier = Modifier.size(PlaceholderIconSize))
            } else {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(modifier = Modifier.height(PhotoToName))
        Text(
            text = member.name,
            style = NameStyle,
            maxLines = NAME_MAX_LINES,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
