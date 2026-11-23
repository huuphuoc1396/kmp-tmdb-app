import SwiftUI
import TmdbCore

private let headerTop: CGFloat = 376
private let headerToDivider: CGFloat = 21
private let dividerToSynopsis: CGFloat = 20
private let synopsisToTrailer: CGFloat = 30
private let toCastHeading: CGFloat = 46
private let castHeadingToRow: CGFloat = 16
private let toSectionHeading: CGFloat = 41
private let headingToContent: CGFloat = 20
private let bottomPadding: CGFloat = 40
private let markerStep: CGFloat = 100
private let scrollSpace = "details.scroll"

struct TitleDetailsView: View {
    @StateObject private var viewModel: TitleDetailsViewModel
    private let titleId: TitleId
    private let onTitleTap: (TitleItem) -> Void

    @Environment(\.openURL) private var openURL

    @SceneStorage private var scrollPosition: String
    @SceneStorage private var castPosition: String
    @SceneStorage private var recommendationsPosition: String

    init(
        titleId: TitleId,
        stackIndex: Int,
        details: any TitleDetailsSource,
        favorites: any Favorites,
        onTitleTap: @escaping (TitleItem) -> Void
    ) {
        _viewModel = StateObject(
            wrappedValue: TitleDetailsViewModel(titleId: titleId, details: details, favorites: favorites)
        )
        self.titleId = titleId
        self.onTitleTap = onTitleTap
        _scrollPosition = SceneStorage(wrappedValue: "", "details.\(stackIndex).scroll")
        _castPosition = SceneStorage(wrappedValue: "", "details.\(stackIndex).cast")
        _recommendationsPosition = SceneStorage(wrappedValue: "", "details.\(stackIndex).recommendations")
    }

    var body: some View {
        let language = HomeLanguage.from(preferredLocalizations: Bundle.main.preferredLocalizations)
        TitleDetailsContent(
            state: viewModel.state,
            language: language,
            onIntent: viewModel.onIntent,
            onRefresh: viewModel.refresh,
            positions: DetailsPositions(
                key: TitleItem.id(for: titleId),
                scroll: $scrollPosition,
                cast: $castPosition,
                recommendations: $recommendationsPosition
            )
        )
        .navigationBarTitleDisplayMode(.inline)
        .toolbar(.visible, for: .navigationBar)
        .toolbarBackground(Theme.track, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                if viewModel.state.details != nil, let isFavorite = viewModel.state.isFavorite {
                    FavoriteToggle(isOn: isFavorite) { viewModel.onIntent(.favoriteTapped) }
                }
            }
        }
        .task {
            viewModel.onIntent(.languageChanged(language))
            for await effect in viewModel.effects {
                switch effect {
                case let .openTrailer(url):
                    openURL(url)
                case let .openTitle(title):
                    onTitleTap(title)
                }
            }
        }
    }
}

struct DetailsPositions {
    let key: String
    @Binding var scroll: String
    @Binding var cast: String
    @Binding var recommendations: String
}

struct TitleDetailsContent: View {
    let state: TitleDetailsUiState
    let language: Language
    let onIntent: (TitleDetailsIntent) -> Void
    let onRefresh: @MainActor @Sendable () async -> Void
    let positions: DetailsPositions

    var body: some View {
        ZStack {
            Theme.page.ignoresSafeArea()
            switch state.load {
            case .loading:
                ProgressView()
                    .controlSize(.large)
                    .tint(Theme.accent)
            case .error:
                RowMessage(message: "details.error", alignment: .center) { onIntent(.retry) }
            case let .loaded(details):
                LoadedContent(
                    details: details,
                    language: language,
                    onIntent: onIntent,
                    onRefresh: onRefresh,
                    positions: positions
                )
            }
        }
    }
}

private struct LoadedContent: View {
    let details: TitleDetailsItem
    let language: Language
    let onIntent: (TitleDetailsIntent) -> Void
    let onRefresh: @MainActor @Sendable () async -> Void
    let positions: DetailsPositions

    @State private var restored = false
    @State private var marker = 0
    @ScaledMetric(relativeTo: .body) private var synopsisSize: CGFloat = 14

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                ZStack(alignment: .top) {
                    DetailsArtwork(url: details.artworkURL)
                    sections(details)
                        .padding(.top, headerTop)
                        .padding(.bottom, bottomPadding)
                }
                .background(alignment: .top) { markers }
                .background(alignment: .top) {
                    GeometryReader { geometry in
                        Color.clear.preference(
                            key: ScrollOffsetKey.self,
                            value: -geometry.frame(in: .named(scrollSpace)).minY
                        )
                    }
                }
            }
            .coordinateSpace(name: scrollSpace)
            .ignoresSafeArea(edges: .top)
            .refreshable { await onRefresh() }
            .onPreferenceChange(ScrollOffsetKey.self) { offset in
                let current = Int(max(0, offset) / markerStep)
                guard restored, current != marker else { return }
                marker = current
                positions.scroll = "\(positions.key)|\(current)"
            }
            .onAppear { restoreScroll(with: proxy) }
        }
    }

    private var markers: some View {
        GeometryReader { geometry in
            VStack(spacing: 0) {
                ForEach(0...Int(geometry.size.height / markerStep), id: \.self) { index in
                    Color.clear
                        .frame(height: markerStep)
                        .id(markerId(index))
                }
            }
        }
        .accessibilityHidden(true)
    }

    private func markerId(_ index: Int) -> String {
        "marker-\(index)"
    }

    private func restoreScroll(with proxy: ScrollViewProxy) {
        guard !restored else { return }
        let prefix = "\(positions.key)|"
        if positions.scroll.hasPrefix(prefix), let saved = Int(positions.scroll.dropFirst(prefix.count)), saved > 0 {
            marker = saved
            DispatchQueue.main.async {
                proxy.scrollTo(markerId(saved), anchor: .top)
                restored = true
            }
        } else {
            restored = true
        }
    }

    private func sections(_ details: TitleDetailsItem) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            DetailsHeader(details: details, language: language)
                .padding(.horizontal, homeScreenPadding)
            Rectangle()
                .fill(Color.black.opacity(0.48))
                .frame(height: 1)
                .padding(.horizontal, homeScreenPadding)
                .padding(.top, headerToDivider)
                .accessibilityHidden(true)
            synopsis(details.synopsis)
                .padding(.horizontal, homeScreenPadding)
                .padding(.top, dividerToSynopsis)
            if details.trailerURL != nil {
                TrailerButton { onIntent(.trailerTapped) }
                    .padding(.horizontal, homeScreenPadding)
                    .padding(.top, synopsisToTrailer)
            }
            if !details.cast.isEmpty {
                SectionHeading("details.cast")
                    .padding(.horizontal, homeScreenPadding)
                    .padding(.top, toCastHeading)
                CastRow(cast: details.cast, rowKey: "\(positions.key)|cast", savedPosition: positions.$cast)
                    .padding(.top, castHeadingToRow)
            }
            if !details.genres.isEmpty {
                SectionHeading("details.genres")
                    .padding(.horizontal, homeScreenPadding)
                    .padding(.top, toSectionHeading)
                GenreChips(genres: details.genres)
                    .padding(.horizontal, homeScreenPadding)
                    .padding(.top, headingToContent)
            }
            if !details.recommendations.isEmpty {
                SectionHeading("details.recommendations")
                    .padding(.horizontal, homeScreenPadding)
                    .padding(.top, toSectionHeading)
                PosterRow(
                    titles: details.recommendations,
                    rowKey: "\(positions.key)|recommendations",
                    savedPosition: positions.$recommendations
                ) { onIntent(.recommendationTapped($0)) }
                    .padding(.top, headingToContent)
            }
        }
    }

    @ViewBuilder
    private func synopsis(_ text: String?) -> some View {
        Group {
            if let text {
                Text(verbatim: text)
            } else {
                Text("details.noSynopsis")
            }
        }
        .font(.system(size: synopsisSize))
        .lineSpacing(synopsisSize * 0.25)
        .foregroundStyle(Theme.synopsis)
        .frame(maxWidth: .infinity, alignment: .leading)
        .fixedSize(horizontal: false, vertical: true)
    }
}

private struct ScrollOffsetKey: PreferenceKey {
    static let defaultValue: CGFloat = 0

    static func reduce(value: inout CGFloat, nextValue: () -> CGFloat) {
        value = nextValue()
    }
}

private let previewDetails = TitleDetailsItem(
    name: "The Unforgivable, a title with a long name that wraps",
    releaseDate: ReleaseDate(year: 2021, month: 12, day: 10),
    runtimeMinutes: 113,
    ratingPercent: 45,
    synopsis: "A woman is released from prison after serving a sentence for a violent crime and re-enters a society "
        + "that refuses to forgive her past.",
    artworkURL: nil,
    genres: ["Drama", "Crime", "Thriller"],
    cast: [
        CastItem(name: "Sandra Bullock", photoURL: nil),
        CastItem(name: "Viola Davis", photoURL: nil),
        CastItem(name: "A cast member with a very long name", photoURL: nil),
    ],
    trailerURL: URL(string: "https://www.youtube.com/watch?v=key"),
    recommendations: [
        TitleItem(id: "movie-1", name: "Recommended", posterURL: nil),
        TitleItem(id: "movie-2", name: "Another one", posterURL: nil),
    ]
)

private let sparseDetails = TitleDetailsItem(
    name: "Sparse",
    releaseDate: nil,
    runtimeMinutes: 45,
    ratingPercent: nil,
    synopsis: nil,
    artworkURL: nil,
    genres: [],
    cast: [],
    trailerURL: nil,
    recommendations: []
)

@MainActor
private func detailsPreview(_ state: TitleDetailsUiState, language: Language = .enUs) -> some View {
    NavigationStack {
        TitleDetailsContent(
            state: state,
            language: language,
            onIntent: { _ in },
            onRefresh: {},
            positions: DetailsPositions(
                key: "movie-1",
                scroll: .constant(""),
                cast: .constant(""),
                recommendations: .constant("")
            )
        )
        .toolbarBackground(Theme.track, for: .navigationBar)
        .toolbarColorScheme(.dark, for: .navigationBar)
    }
}

#Preview("English") {
    detailsPreview(TitleDetailsUiState(load: .loaded(previewDetails), isFavorite: true))
}

#Preview("Portuguese, missing sections") {
    detailsPreview(TitleDetailsUiState(load: .loaded(sparseDetails), isFavorite: false), language: .ptBr)
        .environment(\.locale, Locale(identifier: "pt-BR"))
}

#Preview("Loading") {
    detailsPreview(TitleDetailsUiState())
}

#Preview("Error") {
    detailsPreview(TitleDetailsUiState(load: .error))
}

#Preview("Largest text") {
    detailsPreview(TitleDetailsUiState(load: .loaded(previewDetails), isFavorite: false))
        .environment(\.dynamicTypeSize, .accessibility5)
}
