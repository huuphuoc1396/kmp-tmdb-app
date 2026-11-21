package com.banhmilabs.tmdb.cmp.feature.titledetails.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.service.title.domain.TitleDetails
import com.banhmilabs.tmdb.service.title.domain.TitleDetailsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

/**
 * Shows the details of [titleId] in the device language. They refresh when the language is set (stale ones only),
 * on pull-to-refresh and on retry (always).
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class TitleDetailsViewModel(private val titleId: TitleId, private val repository: TitleDetailsRepository) :
    ViewModel() {
    /** `null` until the screen reports the device language; nothing is observed or refreshed before that. */
    private val language = MutableStateFlow<Language?>(null)

    /** Languages whose last refresh failed, so an error never shows in the other language. */
    private val failed = MutableStateFlow(emptySet<Language>())
    private val manualRefresh = MutableStateFlow(false)

    val state: StateFlow<TitleDetailsUiState> = language
        .filterNotNull()
        .flatMapLatest { current ->
            combine(
                repository.observe(titleId, current),
                failed,
                manualRefresh,
            ) { details, failedLanguages, refreshing ->
                TitleDetailsUiState(load = load(details, current in failedLanguages), isRefreshing = refreshing)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TitleDetailsUiState())

    private val _effects = Channel<TitleDetailsEffect>(Channel.BUFFERED)
    val effects: Flow<TitleDetailsEffect> = _effects.receiveAsFlow()

    init {
        // The repository skips the refresh while the cached details are still fresh.
        language
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { current -> viewModelScope.launch { refresh(current, force = false) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: TitleDetailsIntent) {
        when (intent) {
            is TitleDetailsIntent.LanguageChanged -> language.value = intent.language
            TitleDetailsIntent.BackClicked -> send(TitleDetailsEffect.NavigateBack)
            TitleDetailsIntent.TrailerClicked -> openTrailer()
            is TitleDetailsIntent.RecommendationClicked -> openTitle(intent.title)
            TitleDetailsIntent.Refresh -> refreshManually()
            TitleDetailsIntent.Retry -> retry()
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

    private fun refreshManually() {
        val current = language.value ?: return
        if (manualRefresh.value) return
        manualRefresh.value = true
        viewModelScope.launch {
            try {
                refresh(current, force = true)
            } finally {
                manualRefresh.value = false
            }
        }
    }

    private fun retry() {
        val current = language.value ?: return
        viewModelScope.launch { refresh(current, force = true) }
    }

    private suspend fun refresh(current: Language, force: Boolean) {
        failed.update { it - current }
        val result = repository.refresh(titleId, current, force)
        if (result is AppResult.Failure) failed.update { it + current }
    }

    private fun load(details: TitleDetails?, failed: Boolean): DetailsLoad = when {
        details != null -> DetailsLoad.Loaded(details)
        failed -> DetailsLoad.Error
        else -> DetailsLoad.Loading
    }
}
