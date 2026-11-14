package com.banhmilabs.tmdb.nativeapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banhmilabs.tmdb.service.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

internal class AppViewModel(settingsRepository: SettingsRepository) : ViewModel() {
    val startDestination: StateFlow<StartDestination> = flow {
        val completed = settingsRepository.observeOnboardingCompleted().first()
        if (completed) {
            emit(StartDestination.Home)
        } else {
            emit(StartDestination.Onboarding)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, StartDestination.Loading)
}
