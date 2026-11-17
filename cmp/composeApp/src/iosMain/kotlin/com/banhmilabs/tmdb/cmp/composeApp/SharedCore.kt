package com.banhmilabs.tmdb.cmp.composeApp

import com.banhmilabs.tmdb.cmp.composeApp.di.cmpAppModule
import com.banhmilabs.tmdb.core.di.initKoin

fun setUpSharedCore() {
    initKoin { modules(cmpAppModule) }
}
