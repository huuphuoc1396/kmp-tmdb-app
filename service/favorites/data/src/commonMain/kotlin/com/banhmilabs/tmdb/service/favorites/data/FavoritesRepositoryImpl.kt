package com.banhmilabs.tmdb.service.favorites.data

import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.service.favorites.data.local.FavoritesLocalDataSource
import com.banhmilabs.tmdb.service.favorites.domain.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock

internal class FavoritesRepositoryImpl(private val local: FavoritesLocalDataSource, private val clock: Clock) :
    FavoritesRepository {
    override fun observeFavorites(): Flow<List<TitleId>> = local.observeAll()

    override fun observeIsFavorite(id: TitleId): Flow<Boolean> = local.observe(id)

    override suspend fun setFavorite(id: TitleId, isFavorite: Boolean) {
        if (isFavorite) local.insert(id, addedAt = clock.now()) else local.delete(id)
    }
}
