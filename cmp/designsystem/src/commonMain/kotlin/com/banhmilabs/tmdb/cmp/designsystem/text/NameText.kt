package com.banhmilabs.tmdb.cmp.designsystem.text

import androidx.compose.foundation.layout.Spacer
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

@Composable
fun NameText(
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
    val probe = measure(text, style, TextOverflow.Clip, softWrap = true, maxLines = maxLines, constraints = box)
    val lineEnds = (0 until probe.lineCount).map { line -> probe.getLineEnd(line) }
    val shownLines = wordSafeLineCount(text, lineEnds, maxLines)
    return measure(text, style, TextOverflow.Ellipsis, softWrap = true, maxLines = shownLines, constraints = box)
}
