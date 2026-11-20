package com.banhmilabs.tmdb.service.title.data.details.remote

import com.banhmilabs.tmdb.core.model.Language
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.title.data.details.TitleDetailsEntry
import com.banhmilabs.tmdb.service.title.data.details.TitleDetailsKey

/** Test helpers that map hand-built TMDB details DTOs. */
internal object DetailsDtos {
    val MOVIE_ID = TitleId(550, TitleType.MOVIE)

    fun movie(dto: TmdbTitleDetailsDto, language: Language = Language.EN_US): TitleDetailsEntry =
        checkNotNull(dto.toEntry(TitleDetailsKey(MOVIE_ID, language)))

    fun series(firstAirDate: String? = null, episodeRunTime: List<Int>, lastEpisodeRuntime: Int?): TitleDetailsEntry =
        checkNotNull(
            TmdbTitleDetailsDto(
                name = "Series",
                firstAirDate = firstAirDate,
                episodeRunTime = episodeRunTime,
                lastEpisodeToAir = TmdbEpisodeDto(lastEpisodeRuntime),
            ).toEntry(TitleDetailsKey(TitleId(1399, TitleType.TV), Language.EN_US)),
        )

    fun withVideos(videos: List<TmdbVideoDto>) = TmdbTitleDetailsDto(title = "Film", videos = TmdbVideosDto(videos))

    fun trailer(key: String, language: String, official: Boolean = true) =
        TmdbVideoDto(key = key, site = "YouTube", type = "Trailer", language = language, official = official)
}
