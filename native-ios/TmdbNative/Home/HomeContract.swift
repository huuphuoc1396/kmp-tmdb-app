import TmdbCore

enum RowState: Equatable {
    case loading
    case error
    case titles([TitleItem])
}

struct HomeUiState: Equatable {
    var selectedCategory: TitleCategory = .movies
    var featured: RowState = .loading
    var popular: RowState = .loading
    var isRefreshing = false
    var favorites: [FavoriteTitleItem] = []
    var favoritesCount = 0
    var pendingRemoval: FavoriteTitleItem?
}

enum HomeIntent {
    case languageChanged(Language)
    case categorySelected(TitleCategory)
    case titleTapped(TitleItem)
    case retry(TitleListType)
    case favoriteTapped(FavoriteTitleItem)
    case seeAllFavoritesTapped
    case removeFavoriteTapped(FavoriteTitleItem)
    case removeFavoriteConfirmed
    case removeFavoriteDismissed
}

enum HomeEffect: Equatable {
    case openTitle(TitleItem)
    case openFavorites
}
