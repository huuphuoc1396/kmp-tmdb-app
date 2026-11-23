package com.banhmilabs.tmdb.service.favorites.fake

import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.service.favorites.domain.FavoritesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class FakeFavoritesRepository(initial: Set<TitleId> = emptySet()) : FavoritesRepository {
    private val favorites = MutableStateFlow(initial.toList())
    private val _writes = mutableListOf<Pair<TitleId, Boolean>>()

    val writes: List<Pair<TitleId, Boolean>> get() = _writes.toList()

    override fun observeFavorites(): Flow<List<TitleId>> = favorites

    override fun observeIsFavorite(id: TitleId): Flow<Boolean> = favorites.map { id in it }.distinctUntilChanged()

    override suspend fun setFavorite(id: TitleId, isFavorite: Boolean) {
        _writes += id to isFavorite
        favorites.update {
            when {
                !isFavorite -> it - id
                id in it -> it
                else -> listOf(id) + it
            }
        }
    }
}
