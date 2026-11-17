struct OnboardingUiState: Equatable {
    var isNavigating = false
}

enum OnboardingIntent {
    case getStartedTapped
}

enum OnboardingEffect {
    case navigateToHome
}
