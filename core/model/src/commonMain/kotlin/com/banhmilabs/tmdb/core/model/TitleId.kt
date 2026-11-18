package com.banhmilabs.tmdb.core.model

/** TMDB media type; a TMDB ID is unique only within its type. */
enum class TitleType {
    MOVIE,
    TV,
}

/** Identifies a TMDB title: its ID and its media type. */
data class TitleId(val tmdbId: Long, val type: TitleType)
