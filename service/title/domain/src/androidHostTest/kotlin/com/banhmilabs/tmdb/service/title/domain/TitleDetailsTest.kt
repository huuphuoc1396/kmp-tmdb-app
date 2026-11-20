package com.banhmilabs.tmdb.service.title.domain

import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TitleDetailsTest {
    @Test
    fun releaseYearComesFromDate() {
        assertEquals(2021, details(releaseDate = LocalDate(2021, 12, 10)).releaseYear)
    }

    @Test
    fun releaseYearIsNullWithoutDate() {
        assertNull(details(releaseDate = null).releaseYear)
    }

    @Test
    fun trailerUrlIsYoutubeWatchUrl() {
        assertEquals("https://www.youtube.com/watch?v=abc123", Trailer("abc123").url)
    }

    private fun details(releaseDate: LocalDate?) = TitleDetails(
        id = TitleId(1, TitleType.MOVIE),
        name = "Name",
        releaseDate = releaseDate,
        runtimeMinutes = null,
        ratingPercent = null,
        synopsis = null,
        artworkUrl = null,
        genres = emptyList(),
        cast = emptyList(),
        trailer = null,
        recommendations = emptyList(),
    )
}
