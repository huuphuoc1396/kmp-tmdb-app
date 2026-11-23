package com.banhmilabs.tmdb.service.title.data.details.remote

import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.title.data.details.remote.DetailsDtos.movie
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TitleDetailsArtworkMappingTest {
    @Test
    fun posterIsArtwork() {
        val entry = movie(TmdbTitleDetailsDto(title = "Film", posterPath = "/p.jpg", backdropPath = "/b.jpg"))

        assertEquals("/p.jpg", entry.artworkPath)
        assertTrue(entry.artworkIsPoster)
    }

    @Test
    fun backdropWhenNoPoster() {
        val entry = movie(TmdbTitleDetailsDto(title = "Film", backdropPath = "/b.jpg"))

        assertEquals("/b.jpg", entry.artworkPath)
        assertFalse(entry.artworkIsPoster)
    }

    @Test
    fun noArtwork() {
        assertNull(movie(TmdbTitleDetailsDto(title = "Film")).artworkPath)
    }

    @Test
    fun castIsFirstTenByOrder() {
        val cast = (MAX_CAST + 2 downTo 1).map { TmdbCastDto(name = "Actor $it", order = it) }

        val entry = movie(TmdbTitleDetailsDto(title = "Film", credits = TmdbCreditsDto(cast)))

        assertEquals((1..MAX_CAST).map { "Actor $it" }, entry.cast.map { it.name })
    }

    @Test
    fun recommendationsAreFirstTwentyWithNames() {
        val results = listOf(TmdbRecommendationDto(id = 0)) +
            (1..MAX_RECOMMENDATIONS + 5).map { TmdbRecommendationDto(id = it.toLong(), title = "Film $it") }

        val entry = movie(TmdbTitleDetailsDto(title = "Film", recommendations = TmdbRecommendationsDto(results)))

        assertEquals((1L..MAX_RECOMMENDATIONS).toList(), entry.recommendations.map { it.tmdbId })
    }

    @Test
    fun recommendationTypeFromMediaType() {
        val results = listOf(
            TmdbRecommendationDto(id = 1, name = "Series", mediaType = "tv"),
            TmdbRecommendationDto(id = 2, title = "Film", mediaType = "movie"),
            TmdbRecommendationDto(id = 3, title = "Unknown type"),
        )

        val entry = movie(TmdbTitleDetailsDto(title = "Film", recommendations = TmdbRecommendationsDto(results)))

        assertEquals(listOf(TitleType.TV, TitleType.MOVIE, TitleType.MOVIE), entry.recommendations.map { it.type })
    }
}
