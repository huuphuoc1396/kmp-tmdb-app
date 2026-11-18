package com.banhmilabs.tmdb.core.network

import io.ktor.client.engine.HttpClientEngine

/** The platform's HTTP engine: OkHttp on Android, Darwin (NSURLSession) on iOS. */
internal expect fun platformHttpEngine(): HttpClientEngine
