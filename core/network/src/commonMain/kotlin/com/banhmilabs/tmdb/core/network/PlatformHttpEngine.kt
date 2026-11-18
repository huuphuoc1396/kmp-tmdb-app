package com.banhmilabs.tmdb.core.network

import io.ktor.client.engine.HttpClientEngine

/** OkHttp on Android, Darwin on iOS. */
internal expect fun platformHttpEngine(): HttpClientEngine
