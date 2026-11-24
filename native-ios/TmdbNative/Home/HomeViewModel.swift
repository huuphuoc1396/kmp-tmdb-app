import Foundation
import TmdbCore

let homeFavoritesLimit = 10

@MainActor
final class HomeViewModel: ObservableObject {
    @Published private(set) var state = HomeUiState()

    var effects: AsyncStream<HomeEffect> {
        let (stream, continuation) = AsyncStream.makeStream(of: HomeEffect.self)
        effectsContinuation?.finish()
        effectsContinuation = continuation
        return stream
    }

    private let titleLists: any TitleLists
    private let favoriteTitles: any FavoriteTitles
    private let favorites: any Favorites
    private var effectsContinuation: AsyncStream<HomeEffect>.Continuation?

    private var language: Language?
    private var observed: [TitleListType: [TitleItem]] = [:]
    private var failed: Set<TitleListKey> = []
    private let observations = TaskBag()
    private let favoritesObservation = TaskBag()
    private var pullRefresh: Task<Void, Never>?

    init(titleLists: any TitleLists, favoriteTitles: any FavoriteTitles, favorites: any Favorites) {
        self.titleLists = titleLists
        self.favoriteTitles = favoriteTitles
        self.favorites = favorites
    }

    func onIntent(_ intent: HomeIntent) {
        switch intent {
        case let .languageChanged(language):
            changeLanguage(language)
        case let .categorySelected(category):
            selectCategory(category)
        case let .titleTapped(title):
            effectsContinuation?.yield(.openTitle(title))
        case let .retry(listType):
            retry(listType)
        case let .favoriteTapped(favorite):
            effectsContinuation?.yield(.openTitle(favorite.titleItem))
        case .seeAllFavoritesTapped:
            effectsContinuation?.yield(.openFavorites)
        case let .removeFavoriteTapped(favorite):
            if state.pendingRemoval == nil {
                state.pendingRemoval = favorite
            }
        case .removeFavoriteConfirmed:
            confirmRemoval()
        case .removeFavoriteDismissed:
            state.pendingRemoval = nil
        }
    }

    func refresh() async {
        if let running = pullRefresh {
            await running.value
            return
        }
        guard let featured = key(.featured), let popular = key(.popular) else { return }
        state.isRefreshing = true
        let task = Task {
            async let featuredDone: Void = refreshList(featured, force: true)
            async let popularDone: Void = refreshList(popular, force: true)
            _ = await (featuredDone, popularDone)
        }
        pullRefresh = task
        await task.value
        pullRefresh = nil
        state.isRefreshing = false
    }

    private func changeLanguage(_ newLanguage: Language) {
        guard newLanguage != language else { return }
        language = newLanguage
        observeFavorites(newLanguage)
        showSelection()
    }

    private func observeFavorites(_ language: Language) {
        favoritesObservation.cancelAll()
        let stream = favoriteTitles.observe(language)
        favoritesObservation.add(Task { [weak self] in
            for await favorites in stream {
                guard let self, !Task.isCancelled else { return }
                state.favorites = Array(favorites.prefix(homeFavoritesLimit))
                state.favoritesCount = favorites.count
            }
        })
    }

    private func confirmRemoval() {
        guard let favorite = state.pendingRemoval, let id = favorite.titleItem.titleId else { return }
        state.pendingRemoval = nil
        Task { await favorites.setFavorite(id, false) }
    }

    private func selectCategory(_ category: TitleCategory) {
        guard category != state.selectedCategory else { return }
        state.selectedCategory = category
        showSelection()
    }

    private func showSelection() {
        observations.cancelAll()
        observed = [:]
        updateRows()
        for listType in TitleListType.allCases {
            guard let key = key(listType) else { return }
            let stream = titleLists.observe(key)
            observations.add(Task { [weak self] in
                for await titles in stream {
                    guard let self, !Task.isCancelled else { return }
                    observed[listType] = titles
                    updateRows()
                }
            })
            Task { await refreshList(key, force: false) }
        }
    }

    private func retry(_ listType: TitleListType) {
        guard let key = key(listType) else { return }
        Task { await refreshList(key, force: true) }
    }

    private func refreshList(_ key: TitleListKey, force: Bool) async {
        failed.remove(key)
        updateRows()
        if await !titleLists.refresh(key, force: force) {
            failed.insert(key)
        }
        updateRows()
    }

    private func updateRows() {
        state.featured = rowState(.featured)
        state.popular = rowState(.popular)
    }

    private func rowState(_ listType: TitleListType) -> RowState {
        if let titles = observed[listType] {
            return .titles(titles)
        }
        if let key = key(listType), failed.contains(key) {
            return .error
        }
        return .loading
    }

    private func key(_ listType: TitleListType) -> TitleListKey? {
        language.map { TitleListKey(category: state.selectedCategory, listType: listType, language: $0) }
    }
}

final class TaskBag: @unchecked Sendable {
    private var tasks: [Task<Void, Never>] = []

    func add(_ task: Task<Void, Never>) {
        tasks.append(task)
    }

    func cancelAll() {
        tasks.forEach { $0.cancel() }
        tasks = []
    }

    deinit {
        cancelAll()
    }
}
