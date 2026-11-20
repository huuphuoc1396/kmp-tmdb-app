package com.banhmilabs.tmdb.nativeapp.home

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType
import com.banhmilabs.tmdb.service.title.domain.TitleListsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope

internal val MOVIES_FEATURED = listOf(testTitle(1, TitleType.MOVIE))
internal val MOVIES_POPULAR = listOf(testTitle(2, TitleType.MOVIE), testTitle(3, TitleType.MOVIE))
internal val ANIME_FEATURED = listOf(testTitle(4, TitleType.TV))
internal val ANIME_POPULAR = listOf(testTitle(5, TitleType.TV))

internal fun testTitle(id: Long, type: TitleType) = Title(TitleId(id, type), name = "Title $id", posterUrl = null)

internal data class RefreshRequest(
    val category: TitleCategory,
    val listType: TitleListType,
    val language: Language,
    val force: Boolean,
)

/**
 * A MockK [TitleListsRepository] whose Movies and Anime lists have distinct titles (other lists are empty). Each
 * refresh stores its list's titles for that language, unless [failNextRefresh] was called; every call is recorded.
 */
internal class HomeTestRepository {
    private val lists = mutableMapOf<RefreshRequest, MutableStateFlow<List<Title>?>>()
    private val remote = mapOf(
        (TitleCategory.MOVIES to TitleListType.FEATURED) to MOVIES_FEATURED,
        (TitleCategory.MOVIES to TitleListType.POPULAR) to MOVIES_POPULAR,
        (TitleCategory.ANIME to TitleListType.FEATURED) to ANIME_FEATURED,
        (TitleCategory.ANIME to TitleListType.POPULAR) to ANIME_POPULAR,
    )
    private var nextRefreshError: AppError? = null
    val refreshRequests = mutableListOf<RefreshRequest>()

    val mock: TitleListsRepository = mockk {
        every { observe(any(), any(), any()) } answers { list(firstArg(), secondArg(), thirdArg()) }
        coEvery { refresh(any(), any(), any(), any()) } answers {
            val request = RefreshRequest(firstArg(), secondArg(), thirdArg(), arg(3))
            refreshRequests += request
            val error = nextRefreshError
            if (error != null) {
                nextRefreshError = null
                AppResult.Failure(error)
            } else {
                list(request.category, request.listType, request.language).value =
                    remote[request.category to request.listType].orEmpty()
                AppResult.Success(Unit)
            }
        }
    }

    /** The next refresh fails with [error] and leaves the cache unchanged. */
    fun failNextRefresh(error: AppError) {
        nextRefreshError = error
    }

    private fun list(category: TitleCategory, listType: TitleListType, language: Language) =
        lists.getOrPut(RefreshRequest(category, listType, language, force = false)) { MutableStateFlow(null) }
}

/** Keeps [viewModel]'s state subscribed for the whole test and reports the device [language]. */
internal fun TestScope.start(viewModel: HomeViewModel, language: Language = Language.EN_US) {
    backgroundScope.launch { viewModel.state.collect {} }
    viewModel.onIntent(HomeIntent.LanguageChanged(language))
}
