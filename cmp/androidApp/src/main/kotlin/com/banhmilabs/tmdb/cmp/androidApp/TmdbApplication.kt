package com.banhmilabs.tmdb.cmp.androidApp

import android.app.Application
import com.banhmilabs.tmdb.cmp.composeApp.di.cmpAppModule
import com.banhmilabs.tmdb.core.di.initKoin
import org.koin.android.ext.koin.androidContext

class TmdbApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@TmdbApplication)
            modules(cmpAppModule)
        }
    }
}
