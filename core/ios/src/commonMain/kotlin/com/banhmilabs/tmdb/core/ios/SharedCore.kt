package com.banhmilabs.tmdb.core.ios

import com.banhmilabs.tmdb.core.di.initKoin
import com.banhmilabs.tmdb.service.favorites.domain.FavoritesRepository
import com.banhmilabs.tmdb.service.settings.domain.SettingsRepository
import com.banhmilabs.tmdb.service.title.domain.TitleDetailsRepository
import com.banhmilabs.tmdb.service.title.domain.TitleListsRepository
import org.koin.core.Koin

/**
 * Entry point of the shared core for iOS apps (`SharedCore.shared` in Swift). Call [start] once at launch, then read
 * the domain's repositories from it.
 */
object SharedCore {
    private var koin: Koin? = null

    /** Starts the shared core; later calls do nothing. */
    fun start() {
        if (koin == null) koin = initKoin().koin
    }

    val settingsRepository: SettingsRepository
        get() = checkNotNull(koin) { "Call SharedCore.start() first" }.get()

    val titleListsRepository: TitleListsRepository
        get() = checkNotNull(koin) { "Call SharedCore.start() first" }.get()

    val titleDetailsRepository: TitleDetailsRepository
        get() = checkNotNull(koin) { "Call SharedCore.start() first" }.get()

    val favoritesRepository: FavoritesRepository
        get() = checkNotNull(koin) { "Call SharedCore.start() first" }.get()

    internal fun reset() {
        koin = null
    }
}
