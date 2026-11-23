import Foundation
import TmdbCore

enum DetailsLoad: Equatable {
    case loading
    case error
    case loaded(TitleDetailsItem)
}

struct TitleDetailsUiState: Equatable {
    var load: DetailsLoad = .loading
    var isRefreshing = false
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
