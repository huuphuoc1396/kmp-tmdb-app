import TmdbCore

enum RowState: Equatable {
    /// Nothing cached yet and no failure reported.
    case loading
    /// Nothing cached and the last refresh failed.
    case error
    /// Cached titles; an empty list means the row loaded with nothing in it.
    case titles([TitleItem])
}

struct HomeUiState: Equatable {
    var selectedCategory: TitleCategory = .movies
    var featured: RowState = .loading
    var popular: RowState = .loading
    /// A pull-to-refresh is running; automatic refreshes don't set it.
    var isRefreshing = false
}

enum HomeIntent {
    case languageChanged(Language)
    case categorySelected(TitleCategory)
    case titleTapped(TitleItem)
    case retry(TitleListType)
}

enum HomeEffect: Equatable {
    case openTitle(TitleItem)
}
