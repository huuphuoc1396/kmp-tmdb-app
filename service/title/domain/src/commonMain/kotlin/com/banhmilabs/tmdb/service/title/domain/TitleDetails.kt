package com.banhmilabs.tmdb.service.title.domain

import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import kotlinx.datetime.LocalDate

data class TitleDetails(
    val id: TitleId,
    val name: String,
    val releaseDate: LocalDate?,
    val runtimeMinutes: Int?,
    val ratingPercent: Int?,
    val synopsis: String?,
    val artworkUrl: String?,
    val genres: List<String>,
    val cast: List<CastMember>,
    val trailer: Trailer?,
    val recommendations: List<Title>,
) {
    val releaseYear: Int? get() = releaseDate?.year
}

data class CastMember(val name: String, val photoUrl: String?)

data class Trailer(val youtubeKey: String) {
    val url: String get() = "https://www.youtube.com/watch?v=$youtubeKey"
}
