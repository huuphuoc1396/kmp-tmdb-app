package com.banhmilabs.tmdb.core.common

sealed interface AppError {
    data object Network : AppError

    data object Unauthorized : AppError

    data object NotFound : AppError

    data object Unknown : AppError
}
