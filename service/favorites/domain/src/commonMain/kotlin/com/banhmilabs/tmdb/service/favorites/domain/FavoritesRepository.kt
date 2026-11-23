package com.banhmilabs.tmdb.service.favorites.domain

import com.banhmilabs.tmdb.core.model.TitleId
import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun observeIsFavorite(id: TitleId): Flow<Boolean>

    suspend fun setFavorite(id: TitleId, isFavorite: Boolean)
}
