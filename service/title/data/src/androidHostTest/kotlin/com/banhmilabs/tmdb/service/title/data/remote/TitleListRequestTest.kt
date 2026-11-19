package com.banhmilabs.tmdb.service.title.data.remote

import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class TitleListRequestTest {
    private val now = Instant.parse("2026-11-18T12:00:00Z")

    @Test
    fun moviesFeaturedIsTrendingMovieWeek() {
        assertRequest(TitleCategory.MOVIES, TitleListType.FEATURED, "trending/movie/week", emptyMap(), TitleType.MOVIE)
    }

    @Test
    fun moviesPopularIsMoviePopular() {
        assertRequest(TitleCategory.MOVIES, TitleListType.POPULAR, "movie/popular", emptyMap(), TitleType.MOVIE)
    }

    @Test
    fun seriesFeaturedIsTrendingTvWeek() {
        assertRequest(TitleCategory.SERIES, TitleListType.FEATURED, "trending/tv/week", emptyMap(), TitleType.TV)
    }

    @Test
    fun seriesPopularIsTvPopular() {
        assertRequest(TitleCategory.SERIES, TitleListType.POPULAR, "tv/popular", emptyMap(), TitleType.TV)
    }

    @Test
    fun animeFeaturedAiredInLast7Days() {
        assertRequest(
            TitleCategory.ANIME,
            TitleListType.FEATURED,
            "discover/tv",
            DISCOVER + ANIME + ("air_date.gte" to "2026-11-11"),
            TitleType.TV,
        )
    }

    @Test
    fun animePopular() {
        assertRequest(TitleCategory.ANIME, TitleListType.POPULAR, "discover/tv", DISCOVER + ANIME, TitleType.TV)
    }

    @Test
    fun novelasFeaturedAiredInLast7Days() {
        assertRequest(
            TitleCategory.NOVELAS,
            TitleListType.FEATURED,
            "discover/tv",
            DISCOVER + NOVELAS + ("air_date.gte" to "2026-11-11"),
            TitleType.TV,
        )
    }

    @Test
    fun novelasPopular() {
        assertRequest(TitleCategory.NOVELAS, TitleListType.POPULAR, "discover/tv", DISCOVER + NOVELAS, TitleType.TV)
    }

    @Test
    fun documentariesFeaturedReleasedInLast90Days() {
        assertRequest(
            TitleCategory.DOCUMENTARIES,
            TitleListType.FEATURED,
            "discover/movie",
            DISCOVER + DOCUMENTARIES + ("primary_release_date.gte" to "2026-08-20"),
            TitleType.MOVIE,
        )
    }

    @Test
    fun documentariesPopular() {
        assertRequest(
            TitleCategory.DOCUMENTARIES,
            TitleListType.POPULAR,
            "discover/movie",
            DISCOVER + DOCUMENTARIES,
            TitleType.MOVIE,
        )
    }

    private fun assertRequest(
        category: TitleCategory,
        listType: TitleListType,
        path: String,
        parameters: Map<String, String>,
        type: TitleType,
    ) {
        assertEquals(TitleListRequest(path, parameters, type), titleListRequest(category, listType, now))
    }

    private companion object {
        val DISCOVER = mapOf("include_adult" to "false", "sort_by" to "popularity.desc")
        val ANIME = mapOf("with_genres" to "16", "with_original_language" to "ja")
        val NOVELAS = mapOf("with_genres" to "10766", "with_origin_country" to "BR|MX|CO|AR|VE|CL|PE")
        val DOCUMENTARIES = mapOf("with_genres" to "99")
    }
}
