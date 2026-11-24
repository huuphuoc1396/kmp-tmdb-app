import SwiftUI

struct DetailsRoute: Hashable, Codable {
    let title: TitleItem
    let index: Int
}

enum AppRoute: Hashable, Codable {
    case details(DetailsRoute)
    case favorites
}

struct AppRootView: View {
    @StateObject private var viewModel: AppViewModel
    @State private var path: [AppRoute] = []
    @State private var pathRestored = false
    @SceneStorage("app.path") private var savedPath: Data?
    private let settings: any OnboardingSettings
    private let titleLists: any TitleLists
    private let titleDetails: any TitleDetailsSource
    private let favorites: any Favorites
    private let favoriteTitles: any FavoriteTitles

    init(
        settings: any OnboardingSettings,
        titleLists: any TitleLists,
        titleDetails: any TitleDetailsSource,
        favorites: any Favorites,
        favoriteTitles: any FavoriteTitles
    ) {
        _viewModel = StateObject(wrappedValue: AppViewModel(settings: settings))
        self.settings = settings
        self.titleLists = titleLists
        self.titleDetails = titleDetails
        self.favorites = favorites
        self.favoriteTitles = favoriteTitles
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
                    HomeView(
                        titleLists: titleLists,
                        favoriteTitles: favoriteTitles,
                        favorites: favorites,
                        onTitleTap: open,
                        onSeeAllFavorites: { path.append(.favorites) }
                    )
                    .toolbar(.hidden, for: .navigationBar)
                    .navigationDestination(for: AppRoute.self, destination: destination)
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

    @ViewBuilder
    private func destination(_ route: AppRoute) -> some View {
        switch route {
        case let .details(details):
            if let titleId = details.title.titleId {
                TitleDetailsView(
                    titleId: titleId,
                    stackIndex: details.index,
                    details: titleDetails,
                    favorites: favorites,
                    onTitleTap: open
                )
            }
        case .favorites:
            FavoritesView(favoriteTitles: favoriteTitles, favorites: favorites, onTitleTap: open)
        }
    }

    private func open(_ title: TitleItem) {
        path.append(.details(DetailsRoute(title: title, index: path.count)))
    }

    private func restorePath() {
        guard !pathRestored else { return }
        pathRestored = true
        guard let savedPath, let routes = try? JSONDecoder().decode([AppRoute].self, from: savedPath) else {
            return
        }
        path = routes.filter { route in
            if case let .details(details) = route { return details.title.titleId != nil }
            return true
        }
    }
}
