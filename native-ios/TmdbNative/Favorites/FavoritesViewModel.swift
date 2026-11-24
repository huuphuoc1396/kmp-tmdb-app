import Foundation
import TmdbCore

@MainActor
final class FavoritesViewModel: ObservableObject {
    @Published private(set) var state = FavoritesUiState()

    var effects: AsyncStream<FavoritesEffect> {
        let (stream, continuation) = AsyncStream.makeStream(of: FavoritesEffect.self)
        effectsContinuation?.finish()
        effectsContinuation = continuation
        return stream
    }

    private let favoriteTitles: any FavoriteTitles
    private let favorites: any Favorites
    private var effectsContinuation: AsyncStream<FavoritesEffect>.Continuation?
    private var language: Language?
    private let observation = TaskBag()

    init(favoriteTitles: any FavoriteTitles, favorites: any Favorites) {
        self.favoriteTitles = favoriteTitles
        self.favorites = favorites
    }

    func onIntent(_ intent: FavoritesIntent) {
        switch intent {
        case let .languageChanged(language):
            changeLanguage(language)
        case let .titleTapped(favorite):
            effectsContinuation?.yield(.openTitle(favorite.titleItem))
        case let .removeTapped(favorite):
            if state.pendingRemoval == nil {
                state.pendingRemoval = favorite
            }
        case .removeConfirmed:
            confirmRemoval()
        case .removeDismissed:
            state.pendingRemoval = nil
        }
    }

    private func changeLanguage(_ newLanguage: Language) {
        guard newLanguage != language else { return }
        language = newLanguage
        observation.cancelAll()
        let stream = favoriteTitles.observe(newLanguage)
        observation.add(Task { [weak self] in
            for await favorites in stream {
                guard let self, !Task.isCancelled else { return }
                state.favorites = favorites
            }
        })
    }

    private func confirmRemoval() {
        guard let favorite = state.pendingRemoval, let id = favorite.titleItem.titleId else { return }
        state.pendingRemoval = nil
        Task { await favorites.setFavorite(id, false) }
    }
}
