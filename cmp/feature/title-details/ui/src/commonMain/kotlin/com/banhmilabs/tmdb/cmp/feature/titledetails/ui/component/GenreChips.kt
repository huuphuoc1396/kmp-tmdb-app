package com.banhmilabs.tmdb.cmp.feature.titledetails.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.DetailsColors

private val ChipHeight = 31.dp
private val ChipShape = RoundedCornerShape(34.dp)
private val ChipPadding = 17.5.dp
private val ChipSpacing = 14.dp
private val LineSpacing = 10.dp

private val LabelStyle = TextStyle(color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)

@Composable
internal fun GenreChips(genres: List<String>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ChipSpacing),
        verticalArrangement = Arrangement.spacedBy(LineSpacing),
    ) {
        genres.forEach { genre ->
            Box(
                modifier = Modifier
                    .heightIn(min = ChipHeight)
                    .background(DetailsColors.Track, ChipShape)
                    .padding(horizontal = ChipPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = genre, style = LabelStyle)
            }
        }
    }
}
