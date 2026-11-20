package com.banhmilabs.tmdb.cmp.feature.titledetails.ui.navigation

import com.banhmilabs.tmdb.core.model.TitleId
import com.banhmilabs.tmdb.core.model.TitleType
import kotlin.test.Test
import kotlin.test.assertEquals

class TitleDetailsRouteTest {
    @Test
    fun routeRoundTripsTitleId() {
        val movie = TitleId(550, TitleType.MOVIE)
        val series = TitleId(1399, TitleType.TV)

        assertEquals(TitleDetailsRoute(550, "MOVIE"), movie.toDetailsRoute())
        assertEquals(movie, movie.toDetailsRoute().titleId)
        assertEquals(series, series.toDetailsRoute().titleId)
    }
}
