package com.banhmilabs.tmdb.service.title.data.details.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.banhmilabs.tmdb.core.common.DispatcherProvider
import com.banhmilabs.tmdb.core.model.Title
import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.service.title.data.TmdbImages
import com.banhmilabs.tmdb.service.title.data.details.TitleDetailsEntry
import com.banhmilabs.tmdb.service.title.data.details.TitleDetailsKey
import com.banhmilabs.tmdb.service.title.data.generated.SelectDetails
import com.banhmilabs.tmdb.service.title.data.generated.TitleDatabase
import com.banhmilabs.tmdb.service.title.domain.CastMember
import com.banhmilabs.tmdb.service.title.domain.TitleDetails
import com.banhmilabs.tmdb.service.title.domain.Trailer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Title details cached in SQLDelight, per title and language. The database opens on first use, on the IO dispatcher.
 */
internal class TitleDetailsLocalDataSource(
    private val database: Lazy<TitleDatabase>,
    private val dispatchers: DispatcherProvider,
) {
    private val queries by lazy { database.value.titleDetailsQueries }

    /** Cached details; `null` until they were stored once. */
    fun observe(key: TitleDetailsKey): Flow<TitleDetails?> = flow { emitAll(observeCached(key)) }
        .flowOn(dispatchers.io)

    private fun observeCached(key: TitleDetailsKey): Flow<TitleDetails?> {
        val (tmdbId, type, language) = key.columns()
        val details = queries.selectDetails(tmdbId, type, language).asFlow().mapToOneOrNull(dispatchers.io)
        val genres = queries.selectGenres(tmdbId, type, language).asFlow().mapToList(dispatchers.io)
        val cast = queries.selectCast(tmdbId, type, language) { name, profilePath ->
            CastMember(name = name, photoUrl = profilePath?.let(TmdbImages::profile))
        }.asFlow().mapToList(dispatchers.io)
        val recommendations = queries.selectRecommendations(tmdbId, type, language) { id, recType, name, posterPath ->
            Title(
                id = TitleId(tmdbId = id, type = TitleType.valueOf(recType)),
                name = name,
                posterUrl = posterPath?.let(TmdbImages::poster),
            )
        }.asFlow().mapToList(dispatchers.io)
        return combine(details, genres, cast, recommendations) { row, genreNames, castMembers, titles ->
            row?.toDetails(key.id, genreNames, castMembers, titles)
        }.distinctUntilChanged()
    }

    /** When the details were last stored, or `null` if never. */
    suspend fun fetchedAt(key: TitleDetailsKey): Instant? = withContext(dispatchers.io) {
        val (tmdbId, type, language) = key.columns()
        queries.selectFetchedAt(tmdbId, type, language).executeAsOneOrNull()?.let(Instant::fromEpochMilliseconds)
    }

    /** Replaces the details, their genres, cast and recommendations, and their fetch time in one transaction. */
    suspend fun upsert(key: TitleDetailsKey, entry: TitleDetailsEntry, fetchedAt: Instant): Unit =
        withContext(dispatchers.io) {
            val (tmdbId, type, language) = key.columns()
            database.value.transaction {
                queries.upsertDetails(
                    tmdbId = tmdbId,
                    type = type,
                    language = language,
                    name = entry.name,
                    releaseDate = entry.releaseDate?.toString(),
                    runtimeMinutes = entry.runtimeMinutes?.toLong(),
                    ratingPercent = entry.ratingPercent?.toLong(),
                    synopsis = entry.synopsis,
                    artworkPath = entry.artworkPath,
                    artworkIsPoster = if (entry.artworkIsPoster) 1L else 0L,
                    trailerKey = entry.trailerKey,
                    fetchedAt = fetchedAt.toEpochMilliseconds(),
                )
                queries.deleteGenres(tmdbId, type, language)
                queries.deleteCast(tmdbId, type, language)
                queries.deleteRecommendations(tmdbId, type, language)
                entry.genres.forEachIndexed { position, name ->
                    queries.insertGenre(tmdbId, type, language, position.toLong(), name)
                }
                entry.cast.forEachIndexed { position, member ->
                    queries.insertCast(tmdbId, type, language, position.toLong(), member.name, member.profilePath)
                }
                entry.recommendations.forEachIndexed { position, title ->
                    queries.insertRecommendation(
                        tmdbId = tmdbId,
                        type = type,
                        language = language,
                        position = position.toLong(),
                        recTmdbId = title.tmdbId,
                        recType = title.type.name,
                        name = title.name,
                        posterPath = title.posterPath,
                    )
                }
            }
        }
}

/** The key's database columns: TMDB ID, type name and language tag. */
private fun TitleDetailsKey.columns() = Triple(id.tmdbId, id.type.name, language.tag)

private fun SelectDetails.toDetails(
    id: TitleId,
    genres: List<String>,
    cast: List<CastMember>,
    recommendations: List<Title>,
) = TitleDetails(
    id = id,
    name = name,
    releaseDate = releaseDate?.let(LocalDate::parse),
    runtimeMinutes = runtimeMinutes?.toInt(),
    ratingPercent = ratingPercent?.toInt(),
    synopsis = synopsis,
    artworkUrl = artworkPath?.let { if (artworkIsPoster != 0L) TmdbImages.poster(it) else TmdbImages.backdrop(it) },
    genres = genres,
    cast = cast,
    trailer = trailerKey?.let(::Trailer),
    recommendations = recommendations,
)
