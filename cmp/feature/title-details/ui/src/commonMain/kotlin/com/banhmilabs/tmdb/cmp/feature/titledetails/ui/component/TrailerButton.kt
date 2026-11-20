package com.banhmilabs.tmdb.cmp.feature.titledetails.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.Res
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.details_watch_trailer
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.ic_play_circle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val ButtonHeight = 44.dp
private val ButtonShape = RoundedCornerShape(60.dp)
private val IconSize = 20.dp
private val IconToLabel = 10.dp

private val LabelStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)

/** The full-width "Assistir trailer" pill with the design's purple gradient. */
@Composable
internal fun TrailerButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    // The design's 148.66° gradient runs from the top-left to the bottom-right of the pill.
    val gradient = Brush.linearGradient(
        colors = listOf(colors.primary, colors.primaryContainer),
        start = Offset.Zero,
        end = Offset.Infinite,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .heightIn(min = ButtonHeight)
            .clip(ButtonShape)
            .background(gradient)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_play_circle),
            contentDescription = null,
            modifier = Modifier.size(IconSize),
        )
        Spacer(modifier = Modifier.width(IconToLabel))
        Text(
            text = stringResource(Res.string.details_watch_trailer),
            style = LabelStyle,
            textAlign = TextAlign.Center,
        )
    }
}
