package com.banhmilabs.tmdb.cmp.designsystem.text

internal fun wordSafeLineCount(text: String, lineEnds: List<Int>, maxLines: Int): Int {
    val firstSplit = lineEnds.indexOfFirst { end -> splitsWord(text, end) }
    val shown = if (firstSplit >= 0) firstSplit + 1 else lineEnds.size
    return shown.coerceIn(1, maxLines.coerceAtLeast(1))
}

private fun splitsWord(text: String, end: Int): Boolean =
    end in 1 until text.length && !text[end - 1].isWhitespace() && !text[end].isWhitespace()
