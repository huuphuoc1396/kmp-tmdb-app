package com.banhmilabs.tmdb.nativeapp.titledetails.navigation

import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import kotlinx.serialization.Serializable

/** Opens the details of one title; [type] is a [TitleType] name, so the route has only primitive arguments. */
@Serializable
internal data class TitleDetailsRoute(val tmdbId: Long, val type: String) {
    val titleId: TitleId get() = TitleId(tmdbId, TitleType.valueOf(type))
}

internal fun TitleId.toDetailsRoute(): TitleDetailsRoute = TitleDetailsRoute(tmdbId = tmdbId, type = type.name)
