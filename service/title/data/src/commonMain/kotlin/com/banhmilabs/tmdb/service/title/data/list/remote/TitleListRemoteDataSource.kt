package com.banhmilabs.tmdb.service.title.data.list.remote

import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.model.TitleType
import com.banhmilabs.tmdb.core.network.apiCall
import com.banhmilabs.tmdb.service.title.data.TitleEntry
import com.banhmilabs.tmdb.service.title.data.list.TitleListKey
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlin.time.Instant

/** Fetches title lists from TMDB. */
internal class TitleListRemoteDataSource(private val httpClient: HttpClient) {
    /** Page 1 of the list's agreed endpoint, in TMDB order; entries without a name are dropped. */
    suspend fun fetch(key: TitleListKey, now: Instant): AppResult<List<TitleEntry>> {
        val request = titleListRequest(category = key.category, listType = key.listType, now = now)
        return apiCall {
            httpClient.get(request.path) {
                parameter("language", key.language)
                parameter("page", 1)
                request.parameters.forEach { (name, value) -> parameter(name, value) }
            }.body<TmdbPageDto>().results.mapNotNull { it.toEntry(request.type) }
        }
    }
}

private fun TmdbTitleDto.toEntry(type: TitleType): TitleEntry? = displayName?.let { name ->
    TitleEntry(tmdbId = id, type = type, name = name, posterPath = posterPath)
}
