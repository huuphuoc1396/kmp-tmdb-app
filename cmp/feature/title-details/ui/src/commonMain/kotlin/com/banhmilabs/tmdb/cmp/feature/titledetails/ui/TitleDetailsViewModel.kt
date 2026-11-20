package com.banhmilabs.tmdb.cmp.feature.titledetails.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.service.title.domain.TitleDetailsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

/** Shows the details of [titleId] in the device language, refreshing them (when stale) whenever the language is set. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class TitleDetailsViewModel(private val titleId: TitleId, private val repository: TitleDetailsRepository) :
    ViewModel() {
    /** `null` until the screen reports the device language; nothing is observed or refreshed before that. */
    private val language = MutableStateFlow<Language?>(null)

    val state: StateFlow<TitleDetailsUiState> = language
        .filterNotNull()
        .flatMapLatest { repository.observe(titleId, it) }
        .map(::TitleDetailsUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TitleDetailsUiState())

    private val _effects = Channel<TitleDetailsEffect>(Channel.BUFFERED)
    val effects: Flow<TitleDetailsEffect> = _effects.receiveAsFlow()

    init {
        language
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { current -> viewModelScope.launch { repository.refresh(titleId, current) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: TitleDetailsIntent) {
        when (intent) {
            is TitleDetailsIntent.LanguageChanged -> language.value = intent.language
            TitleDetailsIntent.BackClicked -> send(TitleDetailsEffect.NavigateBack)
            TitleDetailsIntent.TrailerClicked -> openTrailer()
            is TitleDetailsIntent.RecommendationClicked -> openTitle(intent.title)
        }
    }

    private fun openTrailer() {
        val trailer = state.value.details?.trailer ?: return
        send(TitleDetailsEffect.OpenTrailer(trailer.url))
    }

    private fun openTitle(title: Title) = send(TitleDetailsEffect.OpenTitle(title))

    private fun send(effect: TitleDetailsEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}
