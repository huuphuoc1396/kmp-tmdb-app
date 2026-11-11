package com.banhmilabs.tmdb.core.common

/** Outcome of an operation that can fail with an [AppError]. */
sealed interface AppResult<out T> {
    data class Success<out T>(val value: T) : AppResult<T>

    data class Failure(val error: AppError) : AppResult<Nothing>
}
