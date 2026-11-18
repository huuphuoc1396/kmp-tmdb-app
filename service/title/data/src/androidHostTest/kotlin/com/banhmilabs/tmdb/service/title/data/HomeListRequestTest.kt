package com.banhmilabs.tmdb.service.title.data

import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.title.domain.HomeCategory
import com.banhmilabs.tmdb.service.title.domain.HomeRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class HomeListRequestTest {
    private val now = Instant.parse("2026-11-18T12:00:00Z")

    @Test
    fun moviesFeaturedIsTrendingMovieWeek() {
        assertRequest(HomeCategory.MOVIES, HomeRow.FEATURED, "trending/movie/week", emptyMap(), TitleType.MOVIE)
    }

    @Test
    fun moviesPopularIsMoviePopular() {
        assertRequest(HomeCategory.MOVIES, HomeRow.POPULAR, "movie/popular", emptyMap(), TitleType.MOVIE)
    }

    @Test
    fun seriesFeaturedIsTrendingTvWeek() {
        assertRequest(HomeCategory.SERIES, HomeRow.FEATURED, "trending/tv/week", emptyMap(), TitleType.TV)
    }

    @Test
    fun seriesPopularIsTvPopular() {
        assertRequest(HomeCategory.SERIES, HomeRow.POPULAR, "tv/popular", emptyMap(), TitleType.TV)
    }

    @Test
    fun animeFeaturedAiredInLast7Days() {
        assertRequest(
            HomeCategory.ANIME,
            HomeRow.FEATURED,
            "discover/tv",
            DISCOVER + ANIME + ("air_date.gte" to "2026-11-11"),
            TitleType.TV,
        )
    }

    @Test
    fun animePopular() {
        assertRequest(HomeCategory.ANIME, HomeRow.POPULAR, "discover/tv", DISCOVER + ANIME, TitleType.TV)
    }

    @Test
    fun novelasFeaturedAiredInLast7Days() {
        assertRequest(
            HomeCategory.NOVELAS,
            HomeRow.FEATURED,
            "discover/tv",
            DISCOVER + NOVELAS + ("air_date.gte" to "2026-11-11"),
            TitleType.TV,
        )
    }

    @Test
    fun novelasPopular() {
        assertRequest(HomeCategory.NOVELAS, HomeRow.POPULAR, "discover/tv", DISCOVER + NOVELAS, TitleType.TV)
    }

    @Test
    fun documentariesFeaturedReleasedInLast90Days() {
        assertRequest(
            HomeCategory.DOCUMENTARIES,
            HomeRow.FEATURED,
            "discover/movie",
            DISCOVER + DOCUMENTARIES + ("primary_release_date.gte" to "2026-08-20"),
            TitleType.MOVIE,
        )
    }

    @Test
    fun documentariesPopular() {
        assertRequest(
            HomeCategory.DOCUMENTARIES,
            HomeRow.POPULAR,
            "discover/movie",
            DISCOVER + DOCUMENTARIES,
            TitleType.MOVIE,
        )
    }

    private fun assertRequest(
        category: HomeCategory,
        row: HomeRow,
        path: String,
        parameters: Map<String, String>,
        type: TitleType,
    ) {
        assertEquals(HomeListRequest(path, parameters, type), homeListRequest(category, row, now))
    }

    private companion object {
        val DISCOVER = mapOf("include_adult" to "false", "sort_by" to "popularity.desc")
        val ANIME = mapOf("with_genres" to "16", "with_original_language" to "ja")
        val NOVELAS = mapOf("with_genres" to "10766", "with_origin_country" to "BR|MX|CO|AR|VE|CL|PE")
        val DOCUMENTARIES = mapOf("with_genres" to "99")
    }
}
