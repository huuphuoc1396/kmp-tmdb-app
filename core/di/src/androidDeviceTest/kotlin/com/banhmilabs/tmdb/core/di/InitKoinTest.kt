package com.banhmilabs.tmdb.core.di

import androidx.test.platform.app.InstrumentationRegistry
import com.banhmilabs.tmdb.service.favorites.data.generated.FavoritesDatabase
import com.banhmilabs.tmdb.service.favorites.domain.FavoritesRepository
import com.banhmilabs.tmdb.service.settings.domain.SettingsRepository
import com.banhmilabs.tmdb.service.title.data.generated.TitleDatabase
import com.banhmilabs.tmdb.service.title.domain.TitleListsRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InitKoinTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @AfterTest
    fun stop() {
        stopKoin()
        listOf(TITLE_DATABASE, FAVORITES_DATABASE).forEach { name ->
            context.deleteDatabase(name)
            context.noBackupFilesDir.resolve("database-keys/$name.key").delete()
        }
    }

    @Test
    fun initKoinProvidesSettingsRepository() {
        val koin = initKoin { androidContext(context) }.koin

        assertNotNull(koin.getOrNull<SettingsRepository>())
    }

    @Test
    fun initKoinProvidesTitleListsRepository() {
        val koin = initKoin { androidContext(context) }.koin

        assertNotNull(koin.getOrNull<TitleListsRepository>())
    }

    @Test
    fun initKoinProvidesFavoritesRepository() {
        val koin = initKoin { androidContext(context) }.koin

        assertNotNull(koin.getOrNull<FavoritesRepository>())
    }

    @Test
    fun titleDatabaseIsEncrypted() {
        val koin = initKoin { androidContext(context) }.koin
        assertNotNull(koin.getOrNull<TitleListsRepository>())
        koin.get<TitleDatabase>()

        assertEncrypted(TITLE_DATABASE)
    }

    @Test
    fun favoritesDatabaseIsEncrypted() {
        val koin = initKoin { androidContext(context) }.koin
        koin.get<FavoritesDatabase>()

        assertEncrypted(FAVORITES_DATABASE)
    }

    private fun assertEncrypted(name: String) {
        val file = context.getDatabasePath(name)

        assertTrue(file.exists())
        val header = file.readBytes().copyOfRange(0, SQLITE_HEADER.size)
        assertFalse(header.contentEquals(SQLITE_HEADER))
    }

    private companion object {
        const val TITLE_DATABASE = "title.db"
        const val FAVORITES_DATABASE = "favorites.db"
        val SQLITE_HEADER = "SQLite format 3\u0000".encodeToByteArray()
    }
}
