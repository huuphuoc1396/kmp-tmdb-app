package com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui

import androidx.compose.ui.text.font.FontWeight
import kotlin.test.Test
import kotlin.test.assertEquals

class BoldMarkupTest {
    @Test
    fun stripsTagsAndBoldsTheirContent() {
        val text = boldMarkupToAnnotatedString("All about <b>movies</b>, <b>anime</b> and more")

        assertEquals("All about movies, anime and more", text.text)
        val bold = text.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
            .map { text.text.substring(it.start, it.end) }
        assertEquals(listOf("movies", "anime"), bold)
    }

    @Test
    fun plainTextHasNoSpans() {
        val text = boldMarkupToAnnotatedString("Get started")

        assertEquals("Get started", text.text)
        assertEquals(emptyList(), text.spanStyles)
    }

    @Test
    fun unclosedTagRunsToTheEnd() {
        val text = boldMarkupToAnnotatedString("Tudo <b>mais")

        assertEquals("Tudo mais", text.text)
        assertEquals(5 to 9, text.spanStyles.single().let { it.start to it.end })
    }
}
