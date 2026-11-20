package com.banhmilabs.tmdb.service.title.data

import com.banhmilabs.tmdb.core.model.TitleType

/** One title as fetched from TMDB and cached: an item of a list, or a recommendation of a title's details. */
internal data class TitleEntry(val tmdbId: Long, val type: TitleType, val name: String, val posterPath: String?)
