package com.banhmilabs.tmdb.apps.cmp.composeApp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banhmilabs.tmdb.service.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/**
 * The start destination, `null` until known. Read once, not observed: completing onboarding moves to Home by
 * navigation, not by swapping the graph.
 */
class AppViewModel(settingsRepository: SettingsRepository) : ViewModel() {
    val startDestination: StateFlow<StartDestination?> = flow {
        val completed = settingsRepository.observeOnboardingCompleted().first()
        emit(if (completed) StartDestination.Home else StartDestination.Onboarding)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
