package com.banhmilabs.tmdb.nativeapp.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.service.favorites.domain.FavoritesRepository
import com.banhmilabs.tmdb.service.title.domain.FavoriteTitle
import com.banhmilabs.tmdb.service.title.domain.ObserveFavoriteTitles
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

@OptIn(ExperimentalCoroutinesApi::class)
internal class FavoritesViewModel(
    observeFavoriteTitles: ObserveFavoriteTitles,
    private val favoritesRepository: FavoritesRepository,
) : ViewModel() {
    private val language = MutableStateFlow<Language?>(null)
    private val pendingRemoval = MutableStateFlow<FavoriteTitle?>(null)

    val state: StateFlow<FavoritesUiState> = language
        .filterNotNull()
        .flatMapLatest { observeFavoriteTitles(it) }
        .combine(pendingRemoval, ::FavoritesUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), FavoritesUiState())

    private val _effects = Channel<FavoritesEffect>(Channel.BUFFERED)
    val effects: Flow<FavoritesEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: FavoritesIntent) {
        when (intent) {
            is FavoritesIntent.LanguageChanged -> language.value = intent.language

            is FavoritesIntent.TitleClicked -> viewModelScope.launch {
                _effects.send(FavoritesEffect.OpenTitle(intent.id))
            }

            is FavoritesIntent.RemoveFavoriteClicked -> pendingRemoval.compareAndSet(null, intent.favorite)

            FavoritesIntent.RemoveFavoriteConfirmed -> confirmRemoval()

            FavoritesIntent.RemoveFavoriteDismissed -> pendingRemoval.value = null
        }
    }

    private fun confirmRemoval() {
        val favorite = pendingRemoval.getAndUpdate { null } ?: return
        viewModelScope.launch { favoritesRepository.setFavorite(favorite.id, isFavorite = false) }
    }
}
