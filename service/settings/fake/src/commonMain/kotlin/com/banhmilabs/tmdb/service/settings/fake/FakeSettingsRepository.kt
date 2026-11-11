package com.banhmilabs.tmdb.service.settings.fake

import com.banhmilabs.tmdb.core.common.AppError
import com.banhmilabs.tmdb.core.common.AppResult
import com.banhmilabs.tmdb.service.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory [SettingsRepository] for tests; kept honest by [SettingsRepositoryContract]. */
class FakeSettingsRepository(onboardingCompleted: Boolean = false) : SettingsRepository {
    private val completed = MutableStateFlow(onboardingCompleted)
    private var nextCompleteError: AppError? = null

    /** The next [completeOnboarding] fails with [error] and leaves the state unchanged. */
    fun failNextComplete(error: AppError) {
        nextCompleteError = error
    }

    override fun observeOnboardingCompleted(): Flow<Boolean> = completed

    override suspend fun completeOnboarding(): AppResult<Unit> {
        val error = nextCompleteError
        if (error != null) {
            nextCompleteError = null
            return AppResult.Failure(error)
        }
        completed.value = true
        return AppResult.Success(Unit)
    }
}
