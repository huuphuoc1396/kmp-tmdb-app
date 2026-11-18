package com.banhmilabs.tmdb.core.network

import kotlin.test.Test
import kotlin.test.assertSame

class PlatformHttpEngineTest {
    @Test
    fun createsEngine() {
        val engine = platformHttpEngine()
        val client = createTmdbHttpClient(engine = engine, accessToken = "")

        assertSame(engine, client.engine)
        client.close()
    }
}
