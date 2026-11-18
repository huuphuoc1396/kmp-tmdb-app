package com.banhmilabs.tmdb.service.title.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeCategoryTest {
    @Test
    fun categoriesAreInTabOrder() {
        assertEquals(
            listOf(
                HomeCategory.MOVIES,
                HomeCategory.SERIES,
                HomeCategory.ANIME,
                HomeCategory.NOVELAS,
                HomeCategory.DOCUMENTARIES,
            ),
            HomeCategory.entries,
        )
        assertEquals(listOf(HomeRow.FEATURED, HomeRow.POPULAR), HomeRow.entries)
    }
}
