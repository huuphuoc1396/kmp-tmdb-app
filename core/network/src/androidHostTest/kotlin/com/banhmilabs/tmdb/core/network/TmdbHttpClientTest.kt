package com.banhmilabs.tmdb.core.network

import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class TmdbHttpClientTest {
    private val requests = mutableListOf<HttpRequestData>()

    @Test
    fun sendsBearerToken() = runTest {
        client().get("movie/popular")

        assertEquals("Bearer $TOKEN", requests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun requestsAreHttpsToTmdbBaseUrl() = runTest {
        client().get("movie/popular")

        assertEquals("https://api.themoviedb.org/3/movie/popular", requests.single().url.toString())
    }

    @Test
    fun sendsLanguageParameterGivenByCaller() = runTest {
        val client = client()

        client.get("movie/popular") { parameter("language", "pt-BR") }
        client.get("movie/popular") { parameter("language", "en-US") }

        assertEquals(listOf("pt-BR", "en-US"), requests.map { it.url.parameters["language"] })
    }

    @Test
    fun acceptsJson() = runTest {
        client().get("movie/popular")

        assertEquals("application/json", requests.single().headers[HttpHeaders.Accept])
    }

    @Test
    fun decodesJsonIgnoringUnknownKeys() = runTest {
        val body = client { respondJson("""{"id": 7, "name": "Dune", "extra": true}""") }
            .get("movie/7").body<TestTitle>()

        assertEquals(TestTitle(id = 7, name = "Dune"), body)
    }

    private fun client(handler: MockRequestHandleScope.() -> HttpResponseData = { respondJson("{}") }) =
        createTmdbHttpClient(
            engine = MockEngine { request ->
                requests += request
                handler()
            },
            accessToken = TOKEN,
        )

    private fun MockRequestHandleScope.respondJson(json: String) =
        respond(json, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    @Serializable
    private data class TestTitle(val id: Int, val name: String)

    private companion object {
        const val TOKEN = "test-token"
    }
}
