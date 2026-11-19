package com.banhmilabs.tmdb.cmp.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import com.banhmilabs.tmdb.service.title.domain.TitleListsRepository
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
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

/** Shows both lists of the selected category in the device language, refreshing them when they are shown. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class HomeViewModel(private val repository: TitleListsRepository) : ViewModel() {
    private val category = MutableStateFlow(TitleCategory.MOVIES)

    /** `null` until the screen reports the device language; nothing is observed or refreshed before that. */
    private val language = MutableStateFlow<Language?>(null)
    private val selection = combine(category, language.filterNotNull(), ::Selection).distinctUntilChanged()

    val state: StateFlow<HomeUiState> = selection
        .flatMapLatest { (category, language) ->
            combine(
                repository.observe(category = category, listType = TitleListType.FEATURED, language = language),
                repository.observe(category = category, listType = TitleListType.POPULAR, language = language),
            ) { featured, popular ->
                HomeUiState(selectedCategory = category, featured = featured.orEmpty(), popular = popular.orEmpty())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    init {
        // Results are not shown yet: a failed refresh leaves the lists as they are.
        selection
            .onEach { (category, language) ->
                TitleListType.entries.forEach { listType ->
                    viewModelScope.launch {
                        repository.refresh(category = category, listType = listType, language = language)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LanguageChanged -> language.value = intent.language
            is HomeIntent.CategorySelected -> category.value = intent.category
            is HomeIntent.TitleClicked -> openTitle(intent.title)
        }
    }

    private fun openTitle(title: Title) {
        viewModelScope.launch { _effects.send(HomeEffect.OpenTitle(title)) }
    }

    private data class Selection(val category: TitleCategory, val language: Language)
}
