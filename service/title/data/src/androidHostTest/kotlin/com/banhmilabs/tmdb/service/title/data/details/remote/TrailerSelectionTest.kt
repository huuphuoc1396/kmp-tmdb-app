package com.banhmilabs.tmdb.service.title.data.details.remote

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.service.title.data.details.remote.DetailsDtos.movie
import com.banhmilabs.tmdb.service.title.data.details.remote.DetailsDtos.trailer
import com.banhmilabs.tmdb.service.title.data.details.remote.DetailsDtos.withVideos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Which TMDB video becomes the trailer. */
class TrailerSelectionTest {
    @Test
    fun trailerPrefersRequestedLanguage() {
        val videos = listOf(trailer("en1", "en"), trailer("pt1", "pt"))

        assertEquals("pt1", movie(withVideos(videos), Language.PT_BR).trailerKey)
        assertEquals("en1", movie(withVideos(videos), Language.EN_US).trailerKey)
    }

    @Test
    fun trailerFallsBackToEnglish() {
        assertEquals("en1", movie(withVideos(listOf(trailer("en1", "en"))), Language.PT_BR).trailerKey)
    }

    @Test
    fun trailerPrefersOfficial() {
        val videos = listOf(trailer("fan", "en", official = false), trailer("official", "en"))

        assertEquals("official", movie(withVideos(videos)).trailerKey)
    }

    @Test
    fun noYoutubeTrailerGivesNull() {
        val videos = listOf(
            TmdbVideoDto(key = "teaser", site = "YouTube", type = "Teaser", language = "en", official = true),
            TmdbVideoDto(key = "vimeo", site = "Vimeo", type = "Trailer", language = "en", official = true),
            trailer("other", "fr"),
        )

        assertNull(movie(withVideos(videos), Language.PT_BR).trailerKey)
    }
}
