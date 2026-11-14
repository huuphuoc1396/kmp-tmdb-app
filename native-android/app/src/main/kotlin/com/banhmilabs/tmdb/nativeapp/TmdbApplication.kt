package com.banhmilabs.tmdb.nativeapp

import android.app.Application
import com.banhmilabs.tmdb.core.di.initKoin
import com.banhmilabs.tmdb.nativeapp.di.nativeAppModule
import org.koin.android.ext.koin.androidContext

class TmdbApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@TmdbApplication)
            modules(nativeAppModule)
        }
    }
}
