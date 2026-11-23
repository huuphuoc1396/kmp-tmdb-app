package com.banhmilabs.tmdb.service.title.domain

import com.banhmilabs.tmdb.core.model.TitleId

data class FavoriteTitle(val id: TitleId, val summary: TitleSummary?)
