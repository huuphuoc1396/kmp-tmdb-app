package com.banhmilabs.tmdb.service.title.data

private const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"

internal object TmdbImages {
    fun poster(path: String): String = IMAGE_BASE_URL + "w500" + path

    fun backdrop(path: String): String = IMAGE_BASE_URL + "w780" + path

    fun profile(path: String): String = IMAGE_BASE_URL + "w185" + path
}
