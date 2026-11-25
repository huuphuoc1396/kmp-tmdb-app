package com.banhmilabs.tmdb.nativeapp.titledetails.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.banhmilabs.tmdb.nativeapp.R
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.nativeapp.designsystem.preview.TmdbComponentPreviews
import com.banhmilabs.tmdb.nativeapp.designsystem.preview.TmdbPreview

internal const val MAX_RING_FONT_SCALE = 1.15f
private const val START_ANGLE = -90f
private const val DEGREES_PER_PERCENT = 3.6f

internal fun ringFontScale(systemFontScale: Float): Float = minOf(systemFontScale, MAX_RING_FONT_SCALE)

@Composable
internal fun RatingRing(percent: Int?, modifier: Modifier = Modifier) {
    val description = if (percent == null) {
        stringResource(R.string.details_not_rated_description)
    } else {
        stringResource(R.string.details_rating_description, percent)
    }
    val value = percent?.toString() ?: stringResource(R.string.details_not_rated)
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    Box(
        modifier = modifier.size(56.dp).clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = 6.dp.toPx()
            drawCircle(color = colors.surfaceContainerHigh)
            drawCircle(color = colors.surface, radius = size.minDimension / 2 - width)
            if (percent != null) {
                val inset = width / 2
                drawArc(
                    color = colors.secondary,
                    startAngle = START_ANGLE,
                    sweepAngle = percent * DEGREES_PER_PERCENT,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - width, size.height - width),
                    style = Stroke(width = width, cap = StrokeCap.Round),
                )
            }
        }
        CompositionLocalProvider(LocalDensity provides Density(density.density, ringFontScale(density.fontScale))) {
            val style = MaterialTheme.typography.titleMedium
            BasicText(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(value)
                    }
                    if (percent != null) withStyle(SpanStyle(fontWeight = FontWeight.Normal)) { append("%") }
                },
                modifier = Modifier.padding(TmdbSpacing.xs),
                style = style.copy(color = colors.onSurface),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = MaterialTheme.typography.labelSmall.fontSize,
                    maxFontSize = style.fontSize,
                ),
            )
        }
    }
}

@TmdbComponentPreviews
@Composable
private fun RatingRingPreview() {
    TmdbPreview {
        Row(
            modifier = Modifier.padding(TmdbSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(TmdbSpacing.md),
        ) {
            RatingRing(percent = 82)
            RatingRing(percent = 45)
            RatingRing(percent = 12)
            RatingRing(percent = null)
        }
    }
}
