package com.banhmilabs.tmdb.service.title.data.details.remote

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.service.title.data.details.TitleDetailsKey
import com.banhmilabs.tmdb.service.title.data.details.remote.DetailsDtos.movie
import com.banhmilabs.tmdb.service.title.data.details.remote.DetailsDtos.series
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TitleDetailsMappingTest {
    @Test
    fun seriesUsesFirstAirDateAndEpisodeRuntime() {
        val entry = series(firstAirDate = "2011-04-17", episodeRunTime = listOf(57, 60), lastEpisodeRuntime = 80)

        assertEquals(LocalDate(2011, 4, 17), entry.releaseDate)
        assertEquals(57, entry.runtimeMinutes)
    }

    @Test
    fun seriesFallsBackToLastEpisodeRuntime() {
        assertEquals(80, series(episodeRunTime = emptyList(), lastEpisodeRuntime = 80).runtimeMinutes)
    }

    @Test
    fun runtimeUnknownWhenMissingOrZero() {
        assertNull(series(episodeRunTime = emptyList(), lastEpisodeRuntime = null).runtimeMinutes)
        assertNull(movie(TmdbTitleDetailsDto(title = "Film", runtime = 0)).runtimeMinutes)
    }

    @Test
    fun movieUsesReleaseDateAndRuntime() {
        val entry = movie(TmdbTitleDetailsDto(title = "Film", releaseDate = "2021-12-10", runtime = 113))

        assertEquals(LocalDate(2021, 12, 10), entry.releaseDate)
        assertEquals(113, entry.runtimeMinutes)
    }

    @Test
    fun emptyOrInvalidDateIsNull() {
        assertNull(movie(TmdbTitleDetailsDto(title = "Film", releaseDate = "")).releaseDate)
        assertNull(movie(TmdbTitleDetailsDto(title = "Film", releaseDate = "2021-13-40")).releaseDate)
    }

    @Test
    fun ratingIsWholePercent() {
        assertEquals(45, movie(TmdbTitleDetailsDto(title = "Film", voteAverage = 4.5, voteCount = 10)).ratingPercent)
        val rounded = movie(TmdbTitleDetailsDto(title = "Film", voteAverage = 8.438, voteCount = 10))
        assertEquals(84, rounded.ratingPercent)
    }

    @Test
    fun noVotesGivesNullRating() {
        assertNull(movie(TmdbTitleDetailsDto(title = "Film", voteAverage = 0.0, voteCount = 0)).ratingPercent)
    }

    @Test
    fun blankSynopsisIsNull() {
        assertNull(movie(TmdbTitleDetailsDto(title = "Film", overview = "  ")).synopsis)
        assertNull(movie(TmdbTitleDetailsDto(title = "Film")).synopsis)
    }

    @Test
    fun missingNameGivesNull() {
        val key = TitleDetailsKey(DetailsDtos.MOVIE_ID, Language.EN_US)

        assertNull(TmdbTitleDetailsDto(overview = "No name").toEntry(key))
    }
}
