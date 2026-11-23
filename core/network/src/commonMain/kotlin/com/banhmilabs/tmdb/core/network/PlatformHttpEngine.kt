package com.banhmilabs.tmdb.core.network

import io.ktor.client.engine.HttpClientEngine

internal expect fun platformHttpEngine(): HttpClientEngine
