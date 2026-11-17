@testable import TmdbNative

@MainActor
final class OnboardingSettingsStub: OnboardingSettings {
    var completed = false
    var completeError: Error?
    private(set) var completeCalls = 0

    func isOnboardingCompleted() async -> Bool {
        completed
    }

    func completeOnboarding() async throws {
        completeCalls += 1
        if let completeError {
            throw completeError
        }
        completed = true
    }
}
