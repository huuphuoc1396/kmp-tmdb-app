package com.banhmilabs.tmdb.apps.cmp.androidApp

import android.app.Application
import com.banhmilabs.tmdb.apps.cmp.composeApp.di.cmpAppModule
import com.banhmilabs.tmdb.core.di.initKoin
import org.koin.android.ext.koin.androidContext

/** Starts the shared core and the cmp app's bindings with the Android context. */
class TmdbApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@TmdbApplication)
            modules(cmpAppModule)
        }
    }
}
