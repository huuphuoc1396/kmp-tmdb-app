package com.banhmilabs.tmdb.nativeapp.designsystem.text

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.banhmilabs.tmdb.nativeapp.designsystem.TmdbSpacing
import com.banhmilabs.tmdb.nativeapp.designsystem.preview.TmdbComponentPreviews
import com.banhmilabs.tmdb.nativeapp.designsystem.preview.TmdbPreview

private const val NO_BREAK_SPACE = ' '

@Composable
internal fun NameText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    textAlign: TextAlign = TextAlign.Start,
) {
    val measurer = rememberTextMeasurer()
    val merged = style.merge(color = color, textAlign = textAlign)
    val laidOut = remember { LaidOutName() }
    Spacer(
        modifier = modifier
            .semantics { this.text = AnnotatedString(text) }
            .layout { measurable, constraints ->
                val result = measurer.measureWordSafe(text, merged, maxLines, constraints.maxWidth)
                laidOut.result = result
                val width = result.size.width.coerceIn(constraints.minWidth, constraints.maxWidth)
                val height = result.size.height.coerceIn(constraints.minHeight, constraints.maxHeight)
                val placeable = measurable.measure(Constraints.fixed(width, height))
                layout(width, height) { placeable.place(0, 0) }
            }
            .drawBehind { laidOut.result?.let { drawText(it) } },
    )
}

private class LaidOutName {
    var result: TextLayoutResult? = null
}

private fun TextMeasurer.measureWordSafe(
    text: String,
    style: TextStyle,
    maxLines: Int,
    maxWidth: Int,
): TextLayoutResult {
    val box = Constraints(maxWidth = maxWidth)
    val glued = probeLines(text, style, maxLines, box)
    val breakable = text.replace(NO_BREAK_SPACE, ' ')
    val (shownText, lines) = if (breakable != text && glued.splitsWord) {
        breakable to probeLines(breakable, style, maxLines, box).shown
    } else {
        text to glued.shown
    }
    return measure(shownText, style, TextOverflow.Ellipsis, softWrap = true, maxLines = lines, constraints = box)
}

private class ProbedLines(val shown: Int, val laidOut: Int) {
    val splitsWord: Boolean get() = shown < laidOut
}

private fun TextMeasurer.probeLines(text: String, style: TextStyle, maxLines: Int, box: Constraints): ProbedLines {
    val probe = measure(text, style, TextOverflow.Clip, softWrap = true, maxLines = maxLines, constraints = box)
    val lineEnds = (0 until probe.lineCount).map { line -> probe.getLineEnd(line) }
    return ProbedLines(shown = wordSafeLineCount(text, lineEnds, maxLines), laidOut = probe.lineCount)
}

@TmdbComponentPreviews
@Composable
private fun NameTextPreview() {
    TmdbPreview {
        NameText(
            text = "Supercalifragilisticexpialidocious and the Extraordinarily Long Title",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(TmdbSpacing.md).width(160.dp),
            maxLines = 2,
        )
    }
}
