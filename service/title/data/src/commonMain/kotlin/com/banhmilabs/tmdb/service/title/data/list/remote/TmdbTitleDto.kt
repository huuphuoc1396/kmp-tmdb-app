package com.banhmilabs.tmdb.service.title.data.list.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class TmdbPageDto(val results: List<TmdbTitleDto>)

@Serializable
internal data class TmdbTitleDto(
    val id: Long,
    val title: String? = null,
    val name: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
) {
    val displayName: String? get() = title ?: name
}
