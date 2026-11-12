package com.banhmilabs.tmdb.apps.cmp.composeApp

import com.banhmilabs.tmdb.core.di.initKoin

/** Starts the shared core; called once from iosApp at launch. */
fun setUpSharedCore() {
    initKoin()
}
