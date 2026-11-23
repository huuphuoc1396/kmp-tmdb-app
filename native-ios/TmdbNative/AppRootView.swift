import SwiftUI

struct DetailsRoute: Hashable, Codable {
    let title: TitleItem
    let index: Int
}

struct AppRootView: View {
    @StateObject private var viewModel: AppViewModel
    @State private var path: [DetailsRoute] = []
    @State private var pathRestored = false
    @SceneStorage("app.path") private var savedPath: Data?
    private let settings: any OnboardingSettings
    private let titleLists: any TitleLists
    private let titleDetails: any TitleDetailsSource
    private let favorites: any Favorites

    init(
        settings: any OnboardingSettings,
        titleLists: any TitleLists,
        titleDetails: any TitleDetailsSource,
        favorites: any Favorites
    ) {
        _viewModel = StateObject(wrappedValue: AppViewModel(settings: settings))
        self.settings = settings
        self.titleLists = titleLists
        self.titleDetails = titleDetails
        self.favorites = favorites
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
                    HomeView(titleLists: titleLists, onTitleTap: open)
                        .toolbar(.hidden, for: .navigationBar)
                        .navigationDestination(for: DetailsRoute.self) { route in
                            if let titleId = route.title.titleId {
                                TitleDetailsView(
                                    titleId: titleId,
                                    stackIndex: route.index,
                                    details: titleDetails,
                                    favorites: favorites,
                                    onTitleTap: open
                                )
                            }
                        }
                }
                .tint(.white)
                .onAppear(perform: restorePath)
                .onChange(of: path) { path in
                    savedPath = try? JSONEncoder().encode(path)
                }
            }
        }
        .task { await viewModel.load() }
    }

    private func open(_ title: TitleItem) {
        path.append(DetailsRoute(title: title, index: path.count))
    }

    private func restorePath() {
        guard !pathRestored else { return }
        pathRestored = true
        guard let savedPath, let routes = try? JSONDecoder().decode([DetailsRoute].self, from: savedPath) else {
            return
        }
        path = routes.filter { $0.title.titleId != nil }
    }
}
