package com.banhmilabs.tmdb.cmp.feature.favorites.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.service.title.domain.ObserveFavoriteTitles
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

@OptIn(ExperimentalCoroutinesApi::class)
internal class FavoritesViewModel(observeFavoriteTitles: ObserveFavoriteTitles) : ViewModel() {
    private val language = MutableStateFlow<Language?>(null)

    val state: StateFlow<FavoritesUiState> = language
        .filterNotNull()
        .flatMapLatest { observeFavoriteTitles(it) }
        .map(::FavoritesUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), FavoritesUiState())

    private val _effects = Channel<FavoritesEffect>(Channel.BUFFERED)
    val effects: Flow<FavoritesEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: FavoritesIntent) {
        when (intent) {
            is FavoritesIntent.LanguageChanged -> language.value = intent.language

            is FavoritesIntent.TitleClicked -> viewModelScope.launch {
                _effects.send(FavoritesEffect.OpenTitle(intent.id))
            }
        }
    }
}
