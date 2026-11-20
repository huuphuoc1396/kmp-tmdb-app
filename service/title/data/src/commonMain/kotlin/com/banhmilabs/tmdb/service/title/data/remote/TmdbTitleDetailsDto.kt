package com.banhmilabs.tmdb.service.title.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A film or series from `movie/{id}` / `tv/{id}` with `credits`, `videos` and `recommendations` appended. */
@Serializable
internal data class TmdbTitleDetailsDto(
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("first_air_date") val firstAirDate: String? = null,
    val runtime: Int? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    @SerialName("last_episode_to_air") val lastEpisodeToAir: TmdbEpisodeDto? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    val genres: List<TmdbGenreDto> = emptyList(),
    val credits: TmdbCreditsDto = TmdbCreditsDto(),
    val videos: TmdbVideosDto = TmdbVideosDto(),
    val recommendations: TmdbRecommendationsDto = TmdbRecommendationsDto(),
)

@Serializable
internal data class TmdbEpisodeDto(val runtime: Int? = null)

@Serializable
internal data class TmdbGenreDto(val name: String)

@Serializable
internal data class TmdbCreditsDto(val cast: List<TmdbCastDto> = emptyList())

@Serializable
internal data class TmdbCastDto(
    val name: String,
    @SerialName("profile_path") val profilePath: String? = null,
    val order: Int = Int.MAX_VALUE,
)

@Serializable
internal data class TmdbVideosDto(val results: List<TmdbVideoDto> = emptyList())

@Serializable
internal data class TmdbVideoDto(
    val key: String,
    val site: String? = null,
    val type: String? = null,
    @SerialName("iso_639_1") val language: String? = null,
    val official: Boolean = false,
)

@Serializable
internal data class TmdbRecommendationsDto(val results: List<TmdbRecommendationDto> = emptyList())

/** A recommended film (`title`) or series (`name`); [mediaType] is `movie` or `tv`. */
@Serializable
internal data class TmdbRecommendationDto(
    val id: Long,
    val title: String? = null,
    val name: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
)
