package com.banhmilabs.tmdb.service.title.data.list

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.DispatcherProvider
import com.banhmilabs.tmdb.core.network.createTmdbHttpClient
import com.banhmilabs.tmdb.service.title.data.KeyedRefreshes
import com.banhmilabs.tmdb.service.title.data.generated.TitleDatabase
import com.banhmilabs.tmdb.service.title.data.list.local.TitleListLocalDataSource
import com.banhmilabs.tmdb.service.title.data.list.remote.TitleListRemoteDataSource
import com.banhmilabs.tmdb.service.title.domain.TitleListsRepository
import com.banhmilabs.tmdb.service.title.fake.TitleListsRepositoryContract.Companion.CONTRACT_TITLES
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.io.IOException
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * A real [TitleListsRepositoryImpl] over an in-memory database and a `MockEngine` that serves [responseBody] (or
 * [nextError] once), with a controllable clock. Records every request in [requests].
 */
internal class TitleListTestEnvironment {
    val requests = mutableListOf<HttpRequestData>()
    var responseBody = contractJson()
    var nextError: AppError? = null

    /** When set, requests wait for it after [requestStarted] completes. */
    var gate: CompletableDeferred<Unit>? = null
    val requestStarted = CompletableDeferred<Unit>()
    var now: Instant = Instant.parse("2026-11-18T12:00:00Z")

    private val clock = mockk<Clock> { every { now() } answers { now } }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun TestScope.createRepository(): TitleListsRepository {
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
        val dispatchers = mockk<DispatcherProvider> { every { io } returns UnconfinedTestDispatcher(testScheduler) }
        return TitleListsRepositoryImpl(
            remote = TitleListRemoteDataSource(createTmdbHttpClient(engine, accessToken = "token")),
            local = TitleListLocalDataSource(TitleDatabase(driver), dispatchers),
            refreshes = KeyedRefreshes(backgroundScope),
            clock = clock,
        )
    }

    fun fixture(name: String): String =
        checkNotNull(javaClass.getResource("/fixtures/$name")) { "Missing fixture $name" }.readText()

    /** TMDB JSON for [CONTRACT_TITLES]. */
    private fun contractJson(): String = CONTRACT_TITLES.joinToString(prefix = """{"results":[""", postfix = "]}") {
        val poster = it.posterUrl?.let { url -> "\"${url.substringAfter("w500")}\"" } ?: "null"
        """{"id":${it.id.tmdbId},"title":"${it.name}","poster_path":$poster}"""
    }
}
