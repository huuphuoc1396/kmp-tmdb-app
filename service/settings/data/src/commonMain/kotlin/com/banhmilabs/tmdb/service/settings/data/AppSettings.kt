package com.banhmilabs.tmdb.service.settings.data

import kotlinx.serialization.Serializable

@Serializable
internal data class AppSettings(val onboardingCompleted: Boolean = false)
