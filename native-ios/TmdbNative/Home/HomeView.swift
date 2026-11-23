import SwiftUI
import TmdbCore

let homeScreenPadding: CGFloat = 30

private let contentTopPadding: CGFloat = 20
private let contentBottomPadding: CGFloat = 79
private let headerToSearch: CGFloat = 17
private let searchToTabs: CGFloat = 26
private let tabsToFeatured: CGFloat = 32
private let featuredToHeading: CGFloat = 23
private let headingToPopular: CGFloat = 16
private let headerGlowHeight: CGFloat = 522

private let screenBackground = LinearGradient(
    stops: [
        .init(color: Theme.homeGradientTop, location: 0),
        .init(color: Theme.night, location: 0.65394),
    ],
    startPoint: .top,
    endPoint: .bottom
)

private let headerGlow = LinearGradient(
    colors: [Theme.primary, Theme.primary.opacity(0)],
    startPoint: .top,
    endPoint: .bottom
)

struct HomeView: View {
    @StateObject private var viewModel: HomeViewModel
    private let onTitleTap: (TitleItem) -> Void

    @SceneStorage("home.selectedCategory") private var savedCategory = ""
    @SceneStorage("home.featured.position") private var featuredPosition = ""
    @SceneStorage("home.popular.position") private var popularPosition = ""

    init(titleLists: any TitleLists, onTitleTap: @escaping (TitleItem) -> Void) {
        _viewModel = StateObject(wrappedValue: HomeViewModel(titleLists: titleLists))
        self.onTitleTap = onTitleTap
    }

    var body: some View {
        HomeContent(
            state: viewModel.state,
            onIntent: viewModel.onIntent,
            onRefresh: viewModel.refresh,
            featuredPosition: $featuredPosition,
            popularPosition: $popularPosition
        )
        .task {
            if let saved = TitleCategory.allCases.first(where: { $0.name == savedCategory }) {
                viewModel.onIntent(.categorySelected(saved))
            }
            let language = HomeLanguage.from(preferredLocalizations: Bundle.main.preferredLocalizations)
            viewModel.onIntent(.languageChanged(language))
            for await effect in viewModel.effects {
                switch effect {
                case let .openTitle(title):
                    onTitleTap(title)
                }
            }
        }
        .onChange(of: viewModel.state.selectedCategory) { category in
            savedCategory = category.name
        }
    }
}

struct HomeContent: View {
    let state: HomeUiState
    let onIntent: (HomeIntent) -> Void
    let onRefresh: @MainActor @Sendable () async -> Void
    @Binding var featuredPosition: String
    @Binding var popularPosition: String

    @ScaledMetric(relativeTo: .headline) private var headingSize: CGFloat = 18

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                screenBackground
                    .ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        HomeHeader()
                            .padding(.horizontal, homeScreenPadding)
                        SearchField()
                            .padding(.horizontal, homeScreenPadding)
                            .padding(.top, headerToSearch)
                        CategoryTabs(selected: state.selectedCategory) { onIntent(.categorySelected($0)) }
                            .padding(.top, searchToTabs)
                        rows
                            .padding(.top, tabsToFeatured)
                    }
                    .padding(.top, contentTopPadding)
                    .padding(.bottom, contentBottomPadding)
                    .background(alignment: .top) {
                        headerGlow
                            .frame(height: headerGlowHeight)
                            .offset(y: -geometry.safeAreaInsets.top)
                            .accessibilityHidden(true)
                    }
                }
                .refreshable { await onRefresh() }
            }
        }
    }

    @ViewBuilder
    private var rows: some View {
        if state.featured == .loading, state.popular == .loading {
            ProgressView()
                .controlSize(.large)
                .tint(Theme.accent)
                .frame(maxWidth: .infinity, minHeight: rowMinHeight)
        } else {
            row(state.featured, listType: .featured, position: $featuredPosition)
            Text("home.popularHeading")
                .font(.system(size: headingSize, weight: .bold))
                .foregroundStyle(.white)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
                .padding(.horizontal, homeScreenPadding)
                .padding(.top, featuredToHeading)
                .padding(.bottom, headingToPopular)
            row(state.popular, listType: .popular, position: $popularPosition)
        }
    }

    @ViewBuilder
    private func row(_ row: RowState, listType: TitleListType, position: Binding<String>) -> some View {
        switch row {
        case .loading:
            ProgressView()
                .tint(Theme.accent)
                .frame(maxWidth: .infinity, minHeight: rowMinHeight, alignment: .leading)
                .padding(.leading, homeScreenPadding)
        case .error:
            RowMessage(message: "home.rowError") { onIntent(.retry(listType)) }
        case let .titles(titles) where titles.isEmpty:
            RowMessage(message: "home.rowEmpty")
        case let .titles(titles):
            PosterRow(titles: titles, rowKey: state.selectedCategory.name, savedPosition: position) {
                onIntent(.titleTapped($0))
            }
        }
    }
}

private let previewTitles = [
    TitleItem(id: "movie-1", name: "A title with a very long name that wraps and is cut after four lines", posterURL: nil),
    TitleItem(id: "movie-2", name: "Short", posterURL: nil),
    TitleItem(id: "movie-3", name: "Another title", posterURL: nil),
]

@MainActor
private func homePreview(_ state: HomeUiState) -> some View {
    HomeContent(
        state: state,
        onIntent: { _ in },
        onRefresh: {},
        featuredPosition: .constant(""),
        popularPosition: .constant("")
    )
}

#Preview("English") {
    homePreview(HomeUiState(featured: .titles(previewTitles), popular: .titles(previewTitles)))
}

#Preview("Portuguese, error and empty") {
    homePreview(HomeUiState(selectedCategory: .novelas, featured: .titles([]), popular: .error))
        .environment(\.locale, Locale(identifier: "pt-BR"))
}

#Preview("Loading") {
    homePreview(HomeUiState())
}

#Preview("Largest text") {
    homePreview(HomeUiState(featured: .titles(previewTitles), popular: .loading))
        .environment(\.dynamicTypeSize, .accessibility5)
}
