package com.banhmilabs.tmdb.core.model

enum class TitleType {
    MOVIE,
    TV,
}

data class TitleId(val tmdbId: Long, val type: TitleType)
