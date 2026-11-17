import SwiftUI

struct AppRootView: View {
    @StateObject private var viewModel: AppViewModel
    private let settings: any OnboardingSettings

    init(settings: any OnboardingSettings) {
        _viewModel = StateObject(wrappedValue: AppViewModel(settings: settings))
        self.settings = settings
    }

    var body: some View {
        Group {
            switch viewModel.startDestination {
            case .loading:
                Theme.night.ignoresSafeArea()
            case .onboarding:
                OnboardingView(settings: settings, onFinished: viewModel.onboardingFinished)
            case .home:
                PlaceholderHomeView()
            }
        }
        .task { await viewModel.load() }
    }
}
