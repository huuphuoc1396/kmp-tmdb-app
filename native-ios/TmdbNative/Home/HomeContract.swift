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
