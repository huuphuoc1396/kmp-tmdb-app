package com.banhmilabs.tmdb.service.settings.domain

import com.banhmilabs.tmdb.core.common.AppResult
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeOnboardingCompleted(): Flow<Boolean>

    suspend fun completeOnboarding(): AppResult<Unit>
}
