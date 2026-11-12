package com.banhmilabs.tmdb.apps.nativeapp.androidApp

import android.app.Application
import com.banhmilabs.tmdb.core.di.initKoin
import org.koin.android.ext.koin.androidContext

class TmdbApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin { androidContext(this@TmdbApplication) }
    }
}
