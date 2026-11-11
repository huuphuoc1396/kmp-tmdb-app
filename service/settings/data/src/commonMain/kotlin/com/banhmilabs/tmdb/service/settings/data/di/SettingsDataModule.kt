package com.banhmilabs.tmdb.service.settings.data.di

import com.banhmilabs.tmdb.core.common.DispatcherProvider
import com.banhmilabs.tmdb.core.datastore.encryptedDataStore
import com.banhmilabs.tmdb.core.security.masterKeyCipher
import com.banhmilabs.tmdb.service.settings.data.AppSettings
import com.banhmilabs.tmdb.service.settings.data.SettingsRepositoryImpl
import com.banhmilabs.tmdb.service.settings.domain.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

private const val MASTER_KEY_ALIAS = "settings"
internal const val SETTINGS_FILE_NAME = "settings.json"

/** Koin bindings of the settings context. */
val settingsDataModule = module {
    single {
        encryptedDataStore(
            serializer = AppSettings.serializer(),
            defaultValue = AppSettings(),
            path = settingsFilePath(),
            fileSystem = settingsFileSystem,
            cipher = masterKeyCipher(MASTER_KEY_ALIAS),
            scope = CoroutineScope(SupervisorJob() + get<DispatcherProvider>().io),
        )
    }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
}
