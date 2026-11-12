package com.banhmilabs.tmdb.apps.cmp.feature.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.banhmilabs.tmdb.service.settings.domain.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class OnboardingViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState.Content)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    /** Only the first tap counts. Home opens even when saving fails. */
    fun onGetStartedClick() {
        if (_uiState.value != OnboardingUiState.Content) return
        _uiState.value = OnboardingUiState.Navigating
        viewModelScope.launch {
            // Navigating only after the save returns keeps it from being cancelled with this ViewModel.
            settingsRepository.completeOnboarding()
            _uiState.value = OnboardingUiState.Finished
        }
    }
}
