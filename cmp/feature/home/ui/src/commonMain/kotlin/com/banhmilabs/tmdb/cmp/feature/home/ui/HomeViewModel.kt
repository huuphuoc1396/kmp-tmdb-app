package com.banhmilabs.tmdb.cmp.feature.home.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.service.title.domain.FavoriteTitle
import com.banhmilabs.tmdb.service.title.domain.ObserveFavoriteTitles
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import com.banhmilabs.tmdb.service.title.domain.TitleListsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L
internal const val HOME_FAVORITES_LIMIT = 10
internal const val SELECTED_CATEGORY_KEY = "selectedCategory"

@OptIn(ExperimentalCoroutinesApi::class)
internal class HomeViewModel(
    private val repository: TitleListsRepository,
    observeFavoriteTitles: ObserveFavoriteTitles,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val category = MutableStateFlow(
        savedStateHandle.get<String>(SELECTED_CATEGORY_KEY)?.let(TitleCategory::valueOf) ?: TitleCategory.MOVIES,
    )

    private val language = MutableStateFlow<Language?>(null)
    private val selection = combine(category, language.filterNotNull(), ::Selection).distinctUntilChanged()

    private val failed = MutableStateFlow(emptySet<ListKey>())
    private val manualRefresh = MutableStateFlow(false)

    private val favorites: Flow<List<FavoriteTitle>> = language
        .filterNotNull()
        .flatMapLatest { observeFavoriteTitles(it) }
        .onStart { emit(emptyList()) }

    val state: StateFlow<HomeUiState> = selection
        .flatMapLatest { current ->
            combine(
                repository.observe(current.category, TitleListType.FEATURED, current.language),
                repository.observe(current.category, TitleListType.POPULAR, current.language),
                failed,
                manualRefresh,
            ) { featured, popular, failedLists, refreshing ->
                HomeUiState(
                    selectedCategory = current.category,
                    featured = rowState(featured, current.key(TitleListType.FEATURED) in failedLists),
                    popular = rowState(popular, current.key(TitleListType.POPULAR) in failedLists),
                    isRefreshing = refreshing,
                )
            }
        }
        .combine(favorites) { lists, all ->
            lists.copy(favorites = all.take(HOME_FAVORITES_LIMIT), favoritesCount = all.size)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            HomeUiState(selectedCategory = category.value),
        )

    private val _effects = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = _effects.receiveAsFlow()

    init {
        selection
            .onEach { current ->
                TitleListType.entries.forEach { listType ->
                    viewModelScope.launch { refresh(current.key(listType), force = false) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.LanguageChanged -> language.value = intent.language
            is HomeIntent.CategorySelected -> selectCategory(intent.category)
            is HomeIntent.TitleClicked -> openTitle(intent.title)
            HomeIntent.Refresh -> refreshAll()
            is HomeIntent.Retry -> retry(intent.listType)
            is HomeIntent.FavoriteClicked -> send(HomeEffect.OpenFavorite(intent.id))
            HomeIntent.SeeAllFavoritesClicked -> send(HomeEffect.OpenFavorites)
        }
    }

    private fun selectCategory(selected: TitleCategory) {
        category.value = selected
        savedStateHandle[SELECTED_CATEGORY_KEY] = selected.name
    }

    private fun openTitle(title: Title) = send(HomeEffect.OpenTitle(title))

    private fun send(effect: HomeEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun refreshAll() {
        val current = currentSelection() ?: return
        if (manualRefresh.value) return
        manualRefresh.value = true
        viewModelScope.launch {
            try {
                TitleListType.entries
                    .map { listType -> async { refresh(current.key(listType), force = true) } }
                    .awaitAll()
            } finally {
                manualRefresh.value = false
            }
        }
    }

    private fun retry(listType: TitleListType) {
        val current = currentSelection() ?: return
        viewModelScope.launch { refresh(current.key(listType), force = true) }
    }

    private suspend fun refresh(key: ListKey, force: Boolean) {
        failed.update { it - key }
        val result = repository.refresh(
            category = key.category,
            listType = key.listType,
            language = key.language,
            force = force,
        )
        if (result is AppResult.Failure) failed.update { it + key }
    }

    private fun currentSelection(): Selection? = language.value?.let { Selection(category.value, it) }

    private fun rowState(titles: List<Title>?, failed: Boolean): RowState = when {
        titles != null -> RowState.Titles(titles)
        failed -> RowState.Error
        else -> RowState.Loading
    }

    private data class Selection(val category: TitleCategory, val language: Language) {
        fun key(listType: TitleListType) = ListKey(category, listType, language)
    }

    private data class ListKey(val category: TitleCategory, val listType: TitleListType, val language: Language)
}
