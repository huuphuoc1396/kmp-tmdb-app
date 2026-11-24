import TmdbCore

struct FavoritesUiState: Equatable {
    var favorites: [FavoriteTitleItem] = []
    var pendingRemoval: FavoriteTitleItem?
}

enum FavoritesIntent {
    case languageChanged(Language)
    case titleTapped(FavoriteTitleItem)
    case removeTapped(FavoriteTitleItem)
    case removeConfirmed
    case removeDismissed
}

enum FavoritesEffect: Equatable {
    case openTitle(TitleItem)
}
