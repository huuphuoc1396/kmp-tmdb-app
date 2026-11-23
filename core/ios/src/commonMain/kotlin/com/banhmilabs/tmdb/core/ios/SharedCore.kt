package com.banhmilabs.tmdb.core.ios

import com.banhmilabs.tmdb.core.di.initKoin
import com.banhmilabs.tmdb.service.favorites.domain.FavoritesRepository
import com.banhmilabs.tmdb.service.settings.domain.SettingsRepository
import com.banhmilabs.tmdb.service.title.domain.TitleDetailsRepository
import com.banhmilabs.tmdb.service.title.domain.TitleListsRepository
import org.koin.core.Koin

object SharedCore {
    private var koin: Koin? = null

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
