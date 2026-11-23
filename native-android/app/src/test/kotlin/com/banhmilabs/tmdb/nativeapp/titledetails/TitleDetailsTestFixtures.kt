package com.banhmilabs.tmdb.nativeapp.titledetails

import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.title.domain.CastMember
import com.banhmilabs.tmdb.service.title.domain.TitleDetails
import com.banhmilabs.tmdb.service.title.domain.Trailer
import com.banhmilabs.tmdb.service.title.fake.FakeTitleDetailsRepository
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

internal fun titleDetailsTestRepository() = FakeTitleDetailsRepository(mapOf(MOVIE_ID to MOVIE_DETAILS))
