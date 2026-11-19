package com.banhmilabs.tmdb.service.title.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A page of TMDB list results. */
@Serializable
internal data class TmdbPageDto(val results: List<TmdbTitleDto>)

/** A film (`title`) or series (`name`) in a TMDB list. */
@Serializable
internal data class TmdbTitleDto(
    val id: Long,
    val title: String? = null,
    val name: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
) {
    /** Films have `title`, series have `name`. */
    val displayName: String? get() = title ?: name
}
