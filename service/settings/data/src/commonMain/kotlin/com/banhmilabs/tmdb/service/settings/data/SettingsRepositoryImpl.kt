package com.banhmilabs.tmdb.service.settings.data

import androidx.datastore.core.DataStore
import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.core.security.CipherException
import com.banhmilabs.tmdb.service.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import okio.IOException

internal class SettingsRepositoryImpl(private val dataStore: DataStore<AppSettings>) : SettingsRepository {
    override fun observeOnboardingCompleted(): Flow<Boolean> = dataStore.data
        .map { it.onboardingCompleted }
        .catch { if (it is IOException) emit(false) else throw it }
        .distinctUntilChanged()

    @Suppress("SwallowedException")
    override suspend fun completeOnboarding(): AppResult<Unit> = try {
        dataStore.updateData { it.copy(onboardingCompleted = true) }
        AppResult.Success(Unit)
    } catch (e: CipherException) {
        AppResult.Failure(AppError.Unknown)
    } catch (e: IOException) {
        AppResult.Failure(AppError.Unknown)
    }
}
