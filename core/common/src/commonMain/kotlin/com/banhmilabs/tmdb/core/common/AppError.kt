package com.banhmilabs.tmdb.core.common

/** Errors the shared core reports to its callers. */
sealed interface AppError {
    data object Network : AppError

    data object Unauthorized : AppError

    data object NotFound : AppError

    data object Unknown : AppError
}
