package com.banhmilabs.tmdb.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** TMDB API v3 base URL; request paths are relative to it (`movie/popular`). */
const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"

private const val CONNECT_TIMEOUT_MILLIS = 15_000L
private const val REQUEST_TIMEOUT_MILLIS = 30_000L

internal val tmdbJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * Ktor client for TMDB API v3: HTTPS base URL, Bearer token, JSON, timeouts; non-2xx responses throw. Endpoints live
 * in each service's `data` module, which adds `parameter("language", tag)` per request and wraps calls in [apiCall].
 */
fun createTmdbHttpClient(
    engine: HttpClientEngine = platformHttpEngine(),
    accessToken: String = TmdbSecrets.accessToken,
): HttpClient = HttpClient(engine) {
    expectSuccess = true
    defaultRequest {
        url(TMDB_BASE_URL)
        bearerAuth(accessToken)
        accept(ContentType.Application.Json)
    }
    install(ContentNegotiation) {
        json(tmdbJson)
    }
    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
        requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
    }
}
