package com.banhmilabs.tmdb.service.title.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class TitleCategoryTest {
    @Test
    fun categoriesAreInTabOrder() {
        assertEquals(
            listOf(
                TitleCategory.MOVIES,
                TitleCategory.SERIES,
                TitleCategory.ANIME,
                TitleCategory.NOVELAS,
                TitleCategory.DOCUMENTARIES,
            ),
            TitleCategory.entries,
        )
        assertEquals(listOf(TitleListType.FEATURED, TitleListType.POPULAR), TitleListType.entries)
    }
}
