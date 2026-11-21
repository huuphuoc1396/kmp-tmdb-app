package com.banhmilabs.tmdb.service.favorites.domain

import com.banhmilabs.tmdb.core.model.TitleId
import kotlinx.coroutines.flow.Flow

/** The user's favorite titles, stored on the device only and the same in every language. */
interface FavoritesRepository {
    /** Whether [id] is a favorite; emits again on every change. */
    fun observeIsFavorite(id: TitleId): Flow<Boolean>

    /** Marks or unmarks [id]. Marking a title that is already a favorite keeps the time it was first added. */
    suspend fun setFavorite(id: TitleId, isFavorite: Boolean)
}
