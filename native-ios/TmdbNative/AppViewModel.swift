import Foundation

@MainActor
final class AppViewModel: ObservableObject {
    @Published private(set) var startDestination: StartDestination = .loading

    private let settings: any OnboardingSettings

    init(settings: any OnboardingSettings) {
        self.settings = settings
    }

    func load() async {
        guard startDestination == .loading else { return }
        let completed = await settings.isOnboardingCompleted()
        startDestination = completed ? .home : .onboarding
    }

    func onboardingFinished() {
        startDestination = .home
    }
}
