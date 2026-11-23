package com.banhmilabs.tmdb.service.title.data

import com.banhmilabs.tmdb.core.model.TitleType

internal data class TitleEntry(val tmdbId: Long, val type: TitleType, val name: String, val posterPath: String?)
