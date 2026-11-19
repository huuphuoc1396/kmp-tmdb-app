package com.banhmilabs.tmdb.service.title.data.remote

import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

private const val FEATURED_TV_DAYS = 7
private const val FEATURED_DOCUMENTARY_DAYS = 90
private const val GENRE_ANIMATION = "16"
private const val GENRE_SOAP = "10766"
private const val GENRE_DOCUMENTARY = "99"
private const val NOVELA_COUNTRIES = "BR|MX|CO|AR|VE|CL|PE"

/** A TMDB request for a title list; [path] is relative to `TMDB_BASE_URL`, [type] is the type of its results. */
internal data class TitleListRequest(val path: String, val parameters: Map<String, String>, val type: TitleType)

/** The TMDB endpoint and filters of each title list; date filters are counted back from [now]. */
internal fun titleListRequest(category: TitleCategory, listType: TitleListType, now: Instant): TitleListRequest {
    val featured = listType == TitleListType.FEATURED
    return when (category) {
        TitleCategory.MOVIES ->
            TitleListRequest(if (featured) "trending/movie/week" else "movie/popular", emptyMap(), TitleType.MOVIE)

        TitleCategory.SERIES ->
            TitleListRequest(if (featured) "trending/tv/week" else "tv/popular", emptyMap(), TitleType.TV)

        TitleCategory.ANIME -> discoverTv(
            mapOf("with_genres" to GENRE_ANIMATION, "with_original_language" to "ja"),
            airedSince = now.takeIf { featured },
        )

        TitleCategory.NOVELAS -> discoverTv(
            mapOf("with_genres" to GENRE_SOAP, "with_origin_country" to NOVELA_COUNTRIES),
            airedSince = now.takeIf { featured },
        )

        TitleCategory.DOCUMENTARIES -> TitleListRequest(
            "discover/movie",
            DISCOVER + ("with_genres" to GENRE_DOCUMENTARY) + listOfNotNull(
                ("primary_release_date.gte" to now.minusDaysAsDate(FEATURED_DOCUMENTARY_DAYS)).takeIf { featured },
            ),
            TitleType.MOVIE,
        )
    }
}

private val DISCOVER = mapOf("include_adult" to "false", "sort_by" to "popularity.desc")

private fun discoverTv(filters: Map<String, String>, airedSince: Instant?) = TitleListRequest(
    "discover/tv",
    DISCOVER + filters + listOfNotNull(airedSince?.let { "air_date.gte" to it.minusDaysAsDate(FEATURED_TV_DAYS) }),
    TitleType.TV,
)

/** The UTC date [days] before this instant, as `yyyy-MM-dd`. */
internal fun Instant.minusDaysAsDate(days: Int): String = (this - days.days).toString().substringBefore('T')
