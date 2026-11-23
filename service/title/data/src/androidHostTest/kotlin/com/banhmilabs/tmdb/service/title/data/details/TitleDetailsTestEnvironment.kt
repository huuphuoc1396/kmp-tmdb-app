package com.banhmilabs.tmdb.service.title.data.details

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.network.createTmdbHttpClient
import com.banhmilabs.tmdb.core.testing.TestDispatcherProvider
import com.banhmilabs.tmdb.service.title.data.KeyedRefreshes
import com.banhmilabs.tmdb.service.title.data.details.local.TitleDetailsLocalDataSource
import com.banhmilabs.tmdb.service.title.data.details.remote.TitleDetailsRemoteDataSource
import com.banhmilabs.tmdb.service.title.data.generated.TitleDatabase
import com.banhmilabs.tmdb.service.title.domain.TitleDetailsRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.io.IOException
import kotlin.time.Clock
import kotlin.time.Instant

internal class TitleDetailsTestEnvironment {
    val requests = mutableListOf<HttpRequestData>()
    var responseBody = fixture("movie_details.json")
    var nextError: AppError? = null

    var gate: CompletableDeferred<Unit>? = null
    val requestStarted = CompletableDeferred<Unit>()
    var now: Instant = Instant.parse("2026-11-20T12:00:00Z")

    private val clock = object : Clock {
        override fun now(): Instant = now
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun TestScope.createRepository(): TitleDetailsRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { TitleDatabase.Schema.create(it) }
        val engine = MockEngine { request ->
            requests += request
            requestStarted.complete(Unit)
            gate?.await()
            when (nextError.also { nextError = null }) {
                null -> respond(responseBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                AppError.Network -> throw IOException("offline")
                AppError.Unauthorized -> respondError(HttpStatusCode.Unauthorized)
                AppError.NotFound -> respondError(HttpStatusCode.NotFound)
                AppError.Unknown -> respondError(HttpStatusCode.InternalServerError)
            }
        }
        val dispatchers = TestDispatcherProvider(UnconfinedTestDispatcher(testScheduler))
        return TitleDetailsRepositoryImpl(
            remote = TitleDetailsRemoteDataSource(createTmdbHttpClient(engine, accessToken = "token")),
            local = TitleDetailsLocalDataSource(lazyOf(TitleDatabase(driver)), dispatchers),
            refreshes = KeyedRefreshes(backgroundScope),
            clock = clock,
        )
    }

    fun fixture(name: String): String =
        checkNotNull(javaClass.getResource("/fixtures/$name")) { "Missing fixture $name" }.readText()
}
