package com.banhmilabs.tmdb.service.title.data.remote

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.core.network.apiCall
import com.banhmilabs.tmdb.service.title.data.CastEntry
import com.banhmilabs.tmdb.service.title.data.TitleDetailsEntry
import com.banhmilabs.tmdb.service.title.data.TitleDetailsKey
import com.banhmilabs.tmdb.service.title.data.TitleListEntry
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

internal const val MAX_CAST = 10
internal const val MAX_RECOMMENDATIONS = 20
private const val PERCENT = 10
private const val YOUTUBE = "YouTube"
private const val TRAILER = "Trailer"
private const val ENGLISH = "en"

/** Fetches a title's details from TMDB. */
internal class TitleDetailsRemoteDataSource(private val httpClient: HttpClient) {
    /** One request with credits, videos and recommendations appended; a response without a name is Unknown. */
    suspend fun fetch(key: TitleDetailsKey): AppResult<TitleDetailsEntry> {
        val path = when (key.id.type) {
            TitleType.MOVIE -> "movie"
            TitleType.TV -> "tv"
        }
        val result = apiCall {
            httpClient.get("$path/${key.id.tmdbId}") {
                parameter("language", key.language.tag)
                parameter("append_to_response", "credits,videos,recommendations")
                parameter("include_video_language", "${key.language.videoLanguage},$ENGLISH")
            }.body<TmdbTitleDetailsDto>()
        }
        return when (result) {
            is AppResult.Failure -> result

            is AppResult.Success ->
                result.value.toEntry(key)?.let { AppResult.Success(it) } ?: AppResult.Failure(AppError.Unknown)
        }
    }
}

/** ISO 639-1 code of TMDB videos in this language. */
private val Language.videoLanguage: String
    get() = when (this) {
        Language.PT_BR -> "pt"
        Language.EN_US -> ENGLISH
    }

/** Maps TMDB details to the cached entry; `null` without a name. */
internal fun TmdbTitleDetailsDto.toEntry(key: TitleDetailsKey): TitleDetailsEntry? {
    val displayName = title ?: name ?: return null
    return TitleDetailsEntry(
        name = displayName,
        releaseDate = (releaseDate ?: firstAirDate)?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
        runtimeMinutes = runtimeMinutes(key.id.type)?.takeIf { it > 0 },
        ratingPercent = if (voteCount == 0) null else (voteAverage * PERCENT).roundToInt(),
        synopsis = overview?.takeIf { it.isNotBlank() },
        artworkPath = posterPath ?: backdropPath,
        artworkIsPoster = posterPath != null,
        trailerKey = videos.results.trailerKey(key.language.videoLanguage),
        genres = genres.map { it.name },
        cast = credits.cast.sortedBy { it.order }.take(MAX_CAST).map { CastEntry(it.name, it.profilePath) },
        recommendations = recommendations.results
            .mapNotNull { it.toEntry(defaultType = key.id.type) }
            .take(MAX_RECOMMENDATIONS),
    )
}

/** A film's runtime; for a series, one episode's: `episode_run_time`, else the latest aired episode's. */
private fun TmdbTitleDetailsDto.runtimeMinutes(type: TitleType): Int? = when (type) {
    TitleType.MOVIE -> runtime
    TitleType.TV -> episodeRunTime.firstOrNull() ?: lastEpisodeToAir?.runtime
}

/** The YouTube trailer in [language], else in English; official videos first, then TMDB order. */
private fun List<TmdbVideoDto>.trailerKey(language: String): String? {
    val trailers = filter { it.site == YOUTUBE && it.type == TRAILER }
    fun inLanguage(code: String) = trailers.filter { it.language == code }.sortedByDescending { it.official }
    return (inLanguage(language).firstOrNull() ?: inLanguage(ENGLISH).firstOrNull())?.key
}

private fun TmdbRecommendationDto.toEntry(defaultType: TitleType): TitleListEntry? {
    val displayName = title ?: name ?: return null
    val type = when (mediaType) {
        "movie" -> TitleType.MOVIE
        "tv" -> TitleType.TV
        else -> defaultType
    }
    return TitleListEntry(tmdbId = id, type = type, name = displayName, posterPath = posterPath)
}
