package com.banhmilabs.tmdb.apps.cmp.composeApp

import com.banhmilabs.tmdb.apps.cmp.composeApp.di.cmpAppModule
import com.banhmilabs.tmdb.core.di.initKoin

fun setUpSharedCore() {
    initKoin { modules(cmpAppModule) }
}
