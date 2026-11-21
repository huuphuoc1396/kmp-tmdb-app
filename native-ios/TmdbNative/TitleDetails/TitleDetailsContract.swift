import Foundation
import TmdbCore

enum DetailsLoad: Equatable {
    /// Nothing cached in the app language and no failure reported.
    case loading
    /// Nothing cached in the app language and its last refresh failed.
    case error
    /// Cached details; shown even when a later refresh fails.
    case loaded(TitleDetailsItem)
}

struct TitleDetailsUiState: Equatable {
    var load: DetailsLoad = .loading
    /// A pull-to-refresh is running; automatic refreshes don't set it.
    var isRefreshing = false
    /// Whether the title is a favorite; `nil` until the stored value is known.
    var isFavorite: Bool?

    var details: TitleDetailsItem? {
        if case let .loaded(details) = load { return details }
        return nil
    }
}

enum TitleDetailsIntent {
    case languageChanged(Language)
    case trailerTapped
    case recommendationTapped(TitleItem)
    case retry
    case favoriteTapped
}

enum TitleDetailsEffect: Equatable {
    case openTrailer(URL)
    case openTitle(TitleItem)
}
