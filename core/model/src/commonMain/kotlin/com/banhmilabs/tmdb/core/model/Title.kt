package com.banhmilabs.tmdb.core.model

/** A film or series as listed: name in the requested language and poster image URL, `null` when TMDB has none. */
data class Title(val id: TitleId, val name: String, val posterUrl: String?)
