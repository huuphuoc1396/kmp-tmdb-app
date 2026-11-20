package com.banhmilabs.tmdb.service.title.data.list

import com.banhmilabs.tmdb.service.title.domain.TitleCategory
import com.banhmilabs.tmdb.service.title.domain.TitleListType

/** One title list in one language; [list] and [language] are its database key. */
internal data class TitleListKey(val category: TitleCategory, val listType: TitleListType, val language: String) {
    val list = "${category.name}_${listType.name}"
}
