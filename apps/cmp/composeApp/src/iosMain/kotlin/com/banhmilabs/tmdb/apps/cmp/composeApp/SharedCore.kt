package com.banhmilabs.tmdb.apps.cmp.composeApp

import com.banhmilabs.tmdb.apps.cmp.composeApp.di.cmpAppModule
import com.banhmilabs.tmdb.core.di.initKoin

/** Starts the shared core and the cmp app's bindings; called once from iosApp at launch. */
fun setUpSharedCore() {
    initKoin { modules(cmpAppModule) }
}
