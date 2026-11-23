package com.banhmilabs.tmdb.service.title.data.details

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.service.title.data.TitleEntry
import kotlinx.datetime.LocalDate

internal data class TitleDetailsKey(val id: TitleId, val language: Language)

internal data class TitleDetailsEntry(
    val name: String,
    val releaseDate: LocalDate?,
    val runtimeMinutes: Int?,
    val ratingPercent: Int?,
    val synopsis: String?,
    val artworkPath: String?,
    val artworkIsPoster: Boolean,
    val trailerKey: String?,
    val genres: List<String>,
    val cast: List<CastEntry>,
    val recommendations: List<TitleEntry>,
)

internal data class CastEntry(val name: String, val profilePath: String?)
