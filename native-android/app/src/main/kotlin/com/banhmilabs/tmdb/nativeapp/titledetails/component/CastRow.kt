package com.banhmilabs.tmdb.nativeapp.titledetails.component

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.nativeapp.designsystem.component.PersonIcon
import com.banhmilabs.tmdb.nativeapp.designsystem.preview.TmdbComponentPreviews
import com.banhmilabs.tmdb.nativeapp.designsystem.preview.TmdbPreview
import com.banhmilabs.tmdb.nativeapp.designsystem.text.NameText
import com.banhmilabs.tmdb.nativeapp.titledetails.previewDetails
import com.banhmilabs.tmdb.service.title.domain.CastMember

private val PhotoBorder = 4.dp

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
        horizontalArrangement = Arrangement.spacedBy(TmdbSpacing.xxs),
    ) {
        itemsIndexed(cast, key = { index, member -> "$index-${member.name}" }) { _, member ->
            CastItem(member)
        }
    }
}

@Composable
private fun CastItem(member: CastMember) {
    Column(
        modifier = Modifier.width(76.dp).semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(60.dp + PhotoBorder * 2)
                .border(PhotoBorder, MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                .padding(PhotoBorder)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            val photoUrl = member.photoUrl
            if (photoUrl == null) {
                PersonIcon(modifier = Modifier.size(32.dp))
            } else {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
        Spacer(modifier = Modifier.height(TmdbSpacing.xxs))
        NameText(
            text = member.name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

@TmdbComponentPreviews
@Composable
private fun CastRowPreview() {
    TmdbPreview {
        CastRow(
            cast = previewDetails.cast,
            startPadding = TmdbSpacing.md,
            modifier = Modifier.padding(vertical = TmdbSpacing.md),
        )
    }
}
