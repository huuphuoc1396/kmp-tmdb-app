package com.banhmilabs.tmdb.service.title.domain

import com.banhmilabs.tmdb.core.model.TitleId

data class TitleSummary(
    val id: TitleId,
    val name: String,
    val posterUrl: String?,
    val releaseYear: Int?,
    val firstGenre: String?,
)
