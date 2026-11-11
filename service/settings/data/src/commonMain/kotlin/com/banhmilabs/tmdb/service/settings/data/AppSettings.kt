package com.banhmilabs.tmdb.service.settings.data

import kotlinx.serialization.Serializable

/** Settings as stored on the device (encrypted JSON). */
@Serializable
internal data class AppSettings(val onboardingCompleted: Boolean = false)
