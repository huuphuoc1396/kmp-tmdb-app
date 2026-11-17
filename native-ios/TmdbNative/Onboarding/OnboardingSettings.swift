import TmdbCore

@MainActor
protocol OnboardingSettings {
    func isOnboardingCompleted() async -> Bool
    func completeOnboarding() async throws
}

@MainActor
struct SharedCoreOnboardingSettings: OnboardingSettings {
    func isOnboardingCompleted() async -> Bool {
        for await completed in SharedCore.shared.settingsRepository.observeOnboardingCompleted() {
            return completed.boolValue
        }
        return false
    }

    func completeOnboarding() async throws {
        let repository = SharedCore.shared.settingsRepository
        _ = try await repository.completeOnboarding()
    }
}
