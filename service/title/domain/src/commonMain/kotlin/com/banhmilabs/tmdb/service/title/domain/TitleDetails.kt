package com.banhmilabs.tmdb.service.title.domain

import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import kotlinx.datetime.LocalDate

/**
 * Everything the details screen shows about a film or series, in the requested language. A `null` field is unknown
 * or missing on TMDB.
 */
data class TitleDetails(
    val id: TitleId,
    val name: String,
    /** Release date of a film, first air date of a series. */
    val releaseDate: LocalDate?,
    /** Runtime of a film, or of one episode of a series. */
    val runtimeMinutes: Int?,
    /** TMDB score as a whole percentage; `null` when nobody has voted. */
    val ratingPercent: Int?,
    /** `null` when TMDB has no synopsis in this language. */
    val synopsis: String?,
    /** Poster, or the backdrop when there is no poster. */
    val artworkUrl: String?,
    val genres: List<String>,
    /** The main cast, at most 10, in TMDB order. */
    val cast: List<CastMember>,
    val trailer: Trailer?,
    /** At most 20 titles. */
    val recommendations: List<Title>,
) {
    val releaseYear: Int? get() = releaseDate?.year
}

/** A cast member: name and profile photo URL, `null` when TMDB has none. */
data class CastMember(val name: String, val photoUrl: String?)

/** A YouTube trailer. */
data class Trailer(val youtubeKey: String) {
    val url: String get() = "https://www.youtube.com/watch?v=$youtubeKey"
}
