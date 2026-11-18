package com.banhmilabs.tmdb.core.network

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ApiCallTest {
    @Test
    fun successReturnsValue() = runTest {
        val result = call { respondJson("""{"id": 1}""") }

        assertEquals(AppResult.Success(TestTitle(id = 1)), result)
    }

    @Test
    fun status401MapsToUnauthorized() = runTest {
        assertEquals(AppResult.Failure(AppError.Unauthorized), call { respondError(HttpStatusCode.Unauthorized) })
    }

    @Test
    fun status404MapsToNotFound() = runTest {
        assertEquals(AppResult.Failure(AppError.NotFound), call { respondError(HttpStatusCode.NotFound) })
    }

    @Test
    fun status500MapsToUnknown() = runTest {
        assertEquals(
            AppResult.Failure(AppError.Unknown),
            call { respondError(HttpStatusCode.InternalServerError) },
        )
    }

    @Test
    fun status429MapsToUnknown() = runTest {
        assertEquals(AppResult.Failure(AppError.Unknown), call { respondError(HttpStatusCode.TooManyRequests) })
    }

    @Test
    fun ioExceptionMapsToNetwork() = runTest {
        assertEquals(AppResult.Failure(AppError.Network), call { throw IOException("offline") })
    }

    @Test
    fun timeoutMapsToNetwork() = runTest {
        val result = call {
            delay(REQUEST_TIMEOUT_EXCEEDED_MILLIS)
            respondJson("{}")
        }

        assertEquals(AppResult.Failure(AppError.Network), result)
    }

    @Test
    fun malformedJsonMapsToUnknown() = runTest {
        assertEquals(AppResult.Failure(AppError.Unknown), call { respondJson("""{"id": """) })
    }

    @Test
    fun cancellationIsRethrown() = runTest {
        assertFailsWith<CancellationException> {
            apiCall { throw CancellationException("cancelled") }
        }
    }

    @Test
    fun emptyTokenRequestReturnsUnauthorized() = runTest {
        val result = call(accessToken = "") { respondError(HttpStatusCode.Unauthorized) }

        assertEquals(AppResult.Failure(AppError.Unauthorized), result)
    }

    private suspend fun call(
        accessToken: String = "test-token",
        handler: suspend MockRequestHandleScope.() -> HttpResponseData,
    ): AppResult<TestTitle> {
        val client = createTmdbHttpClient(engine = MockEngine { handler() }, accessToken = accessToken)
        return apiCall { client.get("movie/1").body<TestTitle>() }
    }

    @Serializable
    private data class TestTitle(val id: Int)

    private companion object {
        const val REQUEST_TIMEOUT_EXCEEDED_MILLIS = 31_000L
    }
}

private fun MockRequestHandleScope.respondJson(json: String) =
    respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
