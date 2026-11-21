package com.banhmilabs.tmdb.service.favorites.fake

import com.banhmilabs.tmdb.service.favorites.domain.FavoritesRepository
import kotlinx.coroutines.test.TestScope

class FakeFavoritesRepositoryTest : FavoritesRepositoryContract() {
    override fun TestScope.createRepository(): FavoritesRepository = FakeFavoritesRepository()
}
