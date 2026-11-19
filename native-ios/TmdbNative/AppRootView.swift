import SwiftUI

struct AppRootView: View {
    @StateObject private var viewModel: AppViewModel
    @State private var path: [TitleItem] = []
    private let settings: any OnboardingSettings
    private let titleLists: any TitleLists

    init(settings: any OnboardingSettings, titleLists: any TitleLists) {
        _viewModel = StateObject(wrappedValue: AppViewModel(settings: settings))
        self.settings = settings
        self.titleLists = titleLists
    }

    var body: some View {
        Group {
            switch viewModel.startDestination {
            case .loading:
                Theme.night.ignoresSafeArea()
            case .onboarding:
                OnboardingView(settings: settings, onFinished: viewModel.onboardingFinished)
            case .home:
                NavigationStack(path: $path) {
                    HomeView(titleLists: titleLists) { path.append($0) }
                        .toolbar(.hidden, for: .navigationBar)
                        .navigationDestination(for: TitleItem.self) { TitleDetailsPlaceholderView(title: $0) }
                }
                .tint(.white)
            }
        }
        .task { await viewModel.load() }
    }
}
