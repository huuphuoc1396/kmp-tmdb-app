package com.banhmilabs.tmdb.service.title.data

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import kotlinx.datetime.LocalDate

/** One title's details in one language: the database key of its cached details. */
internal data class TitleDetailsKey(val id: TitleId, val language: Language)

/** A title's details as fetched from TMDB and cached; images are TMDB paths. */
internal data class TitleDetailsEntry(
    val name: String,
    val releaseDate: LocalDate?,
    val runtimeMinutes: Int?,
    val ratingPercent: Int?,
    val synopsis: String?,
    val artworkPath: String?,
    /** Whether [artworkPath] is a poster (else a backdrop). */
    val artworkIsPoster: Boolean,
    val trailerKey: String?,
    val genres: List<String>,
    val cast: List<CastEntry>,
    val recommendations: List<TitleListEntry>,
)

/** A cast member as cached. */
internal data class CastEntry(val name: String, val profilePath: String?)
