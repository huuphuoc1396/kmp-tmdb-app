import Foundation
import TmdbCore

@MainActor
final class TitleDetailsViewModel: ObservableObject {
    @Published private(set) var state = TitleDetailsUiState()

    var effects: AsyncStream<TitleDetailsEffect> {
        let (stream, continuation) = AsyncStream.makeStream(of: TitleDetailsEffect.self)
        effectsContinuation?.finish()
        effectsContinuation = continuation
        return stream
    }

    private let titleId: TitleId
    private let details: any TitleDetailsSource
    private let favorites: any Favorites
    private var effectsContinuation: AsyncStream<TitleDetailsEffect>.Continuation?

    private var language: Language?
    private var observed: TitleDetailsItem?
    private var failed: Set<Language> = []
    private let detailsObservation = TaskBag()
    private let favoriteObservation = TaskBag()
    private var pullRefresh: Task<Void, Never>?

    init(titleId: TitleId, details: any TitleDetailsSource, favorites: any Favorites) {
        self.titleId = titleId
        self.details = details
        self.favorites = favorites
        let stream = favorites.observeIsFavorite(titleId)
        favoriteObservation.add(Task { [weak self] in
            for await isFavorite in stream {
                guard let self, !Task.isCancelled else { return }
                state.isFavorite = isFavorite
            }
        })
    }

    func onIntent(_ intent: TitleDetailsIntent) {
        switch intent {
        case let .languageChanged(language):
            changeLanguage(language)
        case .trailerTapped:
            if let url = state.details?.trailerURL {
                effectsContinuation?.yield(.openTrailer(url))
            }
        case let .recommendationTapped(title):
            effectsContinuation?.yield(.openTitle(title))
        case .retry:
            guard let language else { return }
            Task { await refresh(language, force: true) }
        case .favoriteTapped:
            favoriteTapped()
        case .removeFavoriteConfirmed:
            confirmRemoval()
        case .removeFavoriteDismissed:
            state.isRemoveConfirmationShown = false
        }
    }

    func refresh() async {
        if let running = pullRefresh {
            await running.value
            return
        }
        guard let language, state.details != nil else { return }
        state.isRefreshing = true
        let task = Task { await refresh(language, force: true) }
        pullRefresh = task
        await task.value
        pullRefresh = nil
        state.isRefreshing = false
    }

    private func changeLanguage(_ newLanguage: Language) {
        guard newLanguage != language else { return }
        language = newLanguage
        detailsObservation.cancelAll()
        observed = nil
        updateLoad()
        let stream = details.observe(titleId, language: newLanguage)
        detailsObservation.add(Task { [weak self] in
            for await value in stream {
                guard let self, !Task.isCancelled else { return }
                observed = value
                updateLoad()
            }
        })
        Task { await refresh(newLanguage, force: false) }
    }

    private func favoriteTapped() {
        guard state.details != nil, let current = state.isFavorite else { return }
        if current {
            state.isRemoveConfirmationShown = true
        } else {
            Task { await favorites.setFavorite(titleId, true) }
        }
    }

    private func confirmRemoval() {
        guard state.isRemoveConfirmationShown else { return }
        state.isRemoveConfirmationShown = false
        Task { await favorites.setFavorite(titleId, false) }
    }

    private func refresh(_ language: Language, force: Bool) async {
        failed.remove(language)
        updateLoad()
        if await !details.refresh(titleId, language: language, force: force) {
            failed.insert(language)
        }
        updateLoad()
    }

    private func updateLoad() {
        if let observed {
            state.load = .loaded(observed)
        } else if let language, failed.contains(language) {
            state.load = .error
        } else {
            state.load = .loading
        }
    }
}
