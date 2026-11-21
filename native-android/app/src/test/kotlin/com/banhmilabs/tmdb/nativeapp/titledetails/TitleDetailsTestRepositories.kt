package com.banhmilabs.tmdb.nativeapp.titledetails

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.favorites.domain.FavoritesRepository
import com.banhmilabs.tmdb.service.title.domain.CastMember
import com.banhmilabs.tmdb.service.title.domain.TitleDetails
import com.banhmilabs.tmdb.service.title.domain.TitleDetailsRepository
import com.banhmilabs.tmdb.service.title.domain.Trailer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDate

internal val MOVIE_ID = TitleId(550, TitleType.MOVIE)

internal val MOVIE_DETAILS = TitleDetails(
    id = MOVIE_ID,
    name = "Fight Club",
    releaseDate = LocalDate(1999, 10, 15),
    runtimeMinutes = 139,
    ratingPercent = 84,
    synopsis = "A ticking-time-bomb insomniac and a soap salesman form an underground fight club.",
    artworkUrl = "https://image.tmdb.org/t/p/w780/poster.jpg",
    genres = listOf("Drama"),
    cast = listOf(CastMember(name = "Brad Pitt", photoUrl = null)),
    trailer = Trailer(youtubeKey = "BdJKm16Co6M"),
    recommendations = listOf(Title(TitleId(680, TitleType.MOVIE), name = "Pulp Fiction", posterUrl = null)),
)

internal data class DetailsRefresh(val language: Language, val force: Boolean)

/**
 * A MockK [TitleDetailsRepository] for [MOVIE_ID]. Each refresh stores [remoteDetails] for its language, or fails
 * with NotFound when there are none, unless [failNextRefresh] was called; every call is recorded.
 */
internal class TitleDetailsTestRepository(var remoteDetails: TitleDetails? = MOVIE_DETAILS) {
    private val cached = mutableMapOf<Language, MutableStateFlow<TitleDetails?>>()
    private var nextRefreshError: AppError? = null
    val refreshRequests = mutableListOf<DetailsRefresh>()

    val mock: TitleDetailsRepository = mockk {
        every { observe(MOVIE_ID, any()) } answers { details(secondArg()) }
        coEvery { refresh(MOVIE_ID, any(), any()) } answers {
            val request = DetailsRefresh(secondArg(), thirdArg())
            refreshRequests += request
            val error = nextRefreshError ?: AppError.NotFound.takeIf { remoteDetails == null }
            nextRefreshError = null
            if (error != null) {
                AppResult.Failure(error)
            } else {
                details(request.language).value = remoteDetails
                AppResult.Success(Unit)
            }
        }
    }

    /** The next refresh fails with [error] and leaves the cache unchanged. */
    fun failNextRefresh(error: AppError) {
        nextRefreshError = error
    }

    private fun details(language: Language) = cached.getOrPut(language) { MutableStateFlow(null) }
}

/** A MockK [FavoritesRepository] holding the favorite state of [MOVIE_ID] and recording every write. */
internal class FavoritesTestRepository(isFavorite: Boolean = false) {
    private val favorite = MutableStateFlow(isFavorite)
    val writes = mutableListOf<Boolean>()

    val mock: FavoritesRepository = mockk {
        every { observeIsFavorite(MOVIE_ID) } returns favorite
        coEvery { setFavorite(MOVIE_ID, any()) } answers {
            writes += secondArg<Boolean>()
            favorite.value = secondArg()
        }
    }
}
