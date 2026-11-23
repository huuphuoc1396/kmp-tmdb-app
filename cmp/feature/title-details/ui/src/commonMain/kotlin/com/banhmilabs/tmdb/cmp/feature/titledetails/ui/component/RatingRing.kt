package com.banhmilabs.tmdb.cmp.feature.titledetails.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.DetailsColors
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.Res
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.details_not_rated
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.details_not_rated_description
import com.banhmilabs.tmdb.cmp.feature.titledetails.ui.generated.resources.details_rating_description
import org.jetbrains.compose.resources.stringResource

internal val RingSize = 57.dp
private val RingWidth = 6.dp
private const val START_ANGLE = -90f
private const val DEGREES_PER_PERCENT = 3.6f

private val ValueSize = 15.dp

@Composable
internal fun RatingRing(percent: Int?, modifier: Modifier = Modifier) {
    val description = if (percent == null) {
        stringResource(Res.string.details_not_rated_description)
    } else {
        stringResource(Res.string.details_rating_description, percent)
    }
    val value = percent?.toString() ?: stringResource(Res.string.details_not_rated)
    val arcColor = MaterialTheme.colorScheme.secondary
    val valueSize = with(LocalDensity.current) { ValueSize.toSp() }
    Box(
        modifier = modifier.size(RingSize).clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = RingWidth.toPx()
            drawCircle(color = DetailsColors.Track)
            drawCircle(color = DetailsColors.RingCenter, radius = size.minDimension / 2 - width)
            if (percent != null) {
                val inset = width / 2
                drawArc(
                    color = arcColor,
                    startAngle = START_ANGLE,
                    sweepAngle = percent * DEGREES_PER_PERCENT,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - width, size.height - width),
                    style = Stroke(width = width, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(value)
                }
                if (percent != null) withStyle(SpanStyle(fontWeight = FontWeight.Normal)) { append("%") }
            },
            color = Color.White,
            fontSize = valueSize,
        )
    }
}
