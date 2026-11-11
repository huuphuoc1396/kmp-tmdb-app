package com.banhmilabs.tmdb.service.settings.domain

import com.banhmilabs.tmdb.core.common.AppResult
import kotlinx.coroutines.flow.Flow

/** App settings stored on the device. */
interface SettingsRepository {
    /** `true` once onboarding was completed; `false` when nothing is stored or it can't be read. */
    fun observeOnboardingCompleted(): Flow<Boolean>

    /** Records that onboarding was completed. A failure leaves the stored value unchanged. */
    suspend fun completeOnboarding(): AppResult<Unit>
}
