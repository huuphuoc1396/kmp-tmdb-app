import Foundation

@MainActor
final class OnboardingViewModel: ObservableObject {
    @Published private(set) var state = OnboardingUiState()
    let effects: AsyncStream<OnboardingEffect>

    private let settings: any OnboardingSettings
    private let effectsContinuation: AsyncStream<OnboardingEffect>.Continuation

    init(settings: any OnboardingSettings) {
        self.settings = settings
        (effects, effectsContinuation) = AsyncStream.makeStream()
    }

    func onIntent(_ intent: OnboardingIntent) {
        switch intent {
        case .getStartedTapped:
            getStarted()
        }
    }

    private func getStarted() {
        guard !state.isNavigating else { return }
        state.isNavigating = true
        Task {
            try? await settings.completeOnboarding()
            effectsContinuation.yield(.navigateToHome)
        }
    }
}
