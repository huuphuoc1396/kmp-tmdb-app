package com.banhmilabs.tmdb.nativeapp.home.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banhmilabs.tmdb.nativeapp.R

private val FieldHeight = 46.dp
private val FieldShape = RoundedCornerShape(32.dp)
private val HintStart = 21.dp
private val IconEnd = 16.dp
private const val HINT_ALPHA = 0.5f

private val HintStyle = TextStyle(
    color = Color.White.copy(alpha = HINT_ALPHA),
    fontSize = 15.sp,
    fontWeight = FontWeight.Medium,
)

/** The "Search" field, shown as designed with no action: not focusable, not clickable, skipped by screen readers. */
@Composable
internal fun SearchField(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .heightIn(min = FieldHeight)
            .clip(FieldShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clearAndSetSemantics {}
            .padding(start = HintStart, end = IconEnd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_search_hint),
            modifier = Modifier.weight(1f),
            style = HintStyle,
            maxLines = 1,
        )
        // The design mirrors the exported icon so the handle points down-left.
        Image(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            modifier = Modifier.graphicsLayer(scaleX = -1f),
        )
    }
}
