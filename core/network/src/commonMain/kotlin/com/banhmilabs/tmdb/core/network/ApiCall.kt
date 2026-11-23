package com.banhmilabs.tmdb.core.network

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

@Suppress("TooGenericExceptionCaught")
suspend fun <T> apiCall(block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: ResponseException) {
    AppResult.Failure(
        when (e.response.status) {
            HttpStatusCode.Unauthorized -> AppError.Unauthorized
            HttpStatusCode.NotFound -> AppError.NotFound
            else -> AppError.Unknown
        },
    )
} catch (_: HttpRequestTimeoutException) {
    AppResult.Failure(AppError.Network)
} catch (_: IOException) {
    AppResult.Failure(AppError.Network)
} catch (_: Exception) {
    AppResult.Failure(AppError.Unknown)
}
