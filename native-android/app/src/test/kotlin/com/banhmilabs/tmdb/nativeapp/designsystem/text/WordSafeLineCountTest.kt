package com.banhmilabs.tmdb.nativeapp.designsystem.text

import kotlin.test.Test
import kotlin.test.assertEquals

class WordSafeLineCountTest {

    @Test
    fun linesEndingBetweenWordsKeepTheLimit() {
        val text = "The Lord of the Rings"
        assertEquals(2, wordSafeLineCount(text, lineEnds = listOf(8, 16), maxLines = 2))
    }

    @Test
    fun wordSplitAcrossLinesStopsAtItsFirstLine() {
        val text = "Robert Downey"
        assertEquals(1, wordSafeLineCount(text, lineEnds = listOf(5, 7), maxLines = 2))
    }

    @Test
    fun longWordAfterAShortOneKeepsTheFirstLine() {
        val text = "The Extraordinarily Long"
        assertEquals(2, wordSafeLineCount(text, lineEnds = listOf(4, 16, 20, 24), maxLines = Int.MAX_VALUE))
    }

    @Test
    fun breakAfterHyphenCountsAsInsideAWord() {
        val text = "Spider-Man Returns"
        assertEquals(1, wordSafeLineCount(text, lineEnds = listOf(7, 11, 18), maxLines = 4))
    }

    @Test
    fun fewerLinesThanLimitKeepsAll() {
        val text = "Dr. Strangelove or How I Learned"
        assertEquals(3, wordSafeLineCount(text, lineEnds = listOf(4, 19, 32), maxLines = Int.MAX_VALUE))
    }

    @Test
    fun limitIsNeverExceeded() {
        val text = "One Two Three Four"
        assertEquals(2, wordSafeLineCount(text, lineEnds = listOf(4, 8, 14, 18), maxLines = 2))
    }

    @Test
    fun singleLineIsKept() {
        assertEquals(1, wordSafeLineCount("Doomsday", lineEnds = listOf(8), maxLines = 2))
    }

    @Test
    fun emptyTextKeepsOneLine() {
        assertEquals(1, wordSafeLineCount("", lineEnds = listOf(0), maxLines = 2))
    }
}
