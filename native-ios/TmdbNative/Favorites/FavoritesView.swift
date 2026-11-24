import SwiftUI
import TmdbCore

private let pageColor = Color(red: 0x13 / 255, green: 0x0E / 255, blue: 0x1F / 255)
private let screenPadding: CGFloat = 20
private let topPadding: CGFloat = 16
private let titleToSubtitle: CGFloat = 8
private let subtitleToList: CGFloat = 26
private let cardSpacing: CGFloat = 12
private let emptyTop: CGFloat = 48
private let bottomPadding: CGFloat = 24
private let badgeHeight: CGFloat = 28
private let badgePadding: CGFloat = 12

struct FavoritesView: View {
    @StateObject private var viewModel: FavoritesViewModel
    private let onTitleTap: (TitleItem) -> Void

    @SceneStorage("favorites.scroll") private var savedPosition = ""

    init(favoriteTitles: any FavoriteTitles, favorites: any Favorites, onTitleTap: @escaping (TitleItem) -> Void) {
        _viewModel = StateObject(
            wrappedValue: FavoritesViewModel(favoriteTitles: favoriteTitles, favorites: favorites)
        )
        self.onTitleTap = onTitleTap
    }

    var body: some View {
        FavoritesContent(state: viewModel.state, onIntent: viewModel.onIntent, savedPosition: $savedPosition)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar(.visible, for: .navigationBar)
            .toolbarBackground(Theme.track, for: .navigationBar)
            .toolbarColorScheme(.dark, for: .navigationBar)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    CountBadge(count: viewModel.state.favorites.count)
                }
            }
            .task {
                let language = HomeLanguage.from(preferredLocalizations: Bundle.main.preferredLocalizations)
                viewModel.onIntent(.languageChanged(language))
                for await effect in viewModel.effects {
                    switch effect {
                    case let .openTitle(title):
                        onTitleTap(title)
                    }
                }
            }
    }
}

struct FavoritesContent: View {
    let state: FavoritesUiState
    let onIntent: (FavoritesIntent) -> Void
    @Binding var savedPosition: String

    @State private var visible: [String: Int] = [:]
    @State private var restored = false
    @ScaledMetric(relativeTo: .largeTitle) private var titleSize: CGFloat = 26
    @ScaledMetric(relativeTo: .footnote) private var subtitleSize: CGFloat = 13
    @ScaledMetric(relativeTo: .subheadline) private var emptySize: CGFloat = 14

    var body: some View {
        ZStack {
            pageColor.ignoresSafeArea()
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: cardSpacing) {
                        header
                        ForEach(Array(state.favorites.enumerated()), id: \.element.id) { index, favorite in
                            FavoriteCard(
                                favorite: favorite,
                                onTap: { onIntent(.titleTapped(favorite)) },
                                onRemoveTap: { onIntent(.removeTapped(favorite)) }
                            )
                            .id(favorite.id)
                            .onAppear { track(favorite.id, at: index) }
                            .onDisappear { untrack(favorite.id) }
                        }
                        if state.favorites.isEmpty {
                            Text("favorites.empty")
                                .font(.system(size: emptySize))
                                .foregroundStyle(favoritesSecondaryText)
                                .multilineTextAlignment(.center)
                                .frame(maxWidth: .infinity)
                                .padding(.top, emptyTop)
                        }
                    }
                    .padding(.horizontal, screenPadding)
                    .padding(.top, topPadding)
                    .padding(.bottom, bottomPadding)
                }
                .onChange(of: state.favorites.isEmpty) { _ in restore(proxy) }
                .onAppear { restore(proxy) }
            }
        }
        .removeFavoriteDialog(
            isPresented: state.pendingRemoval != nil,
            titleName: state.pendingRemoval?.name,
            onConfirm: { onIntent(.removeConfirmed) },
            onDismiss: { onIntent(.removeDismissed) }
        )
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: titleToSubtitle) {
            Text("favorites.title")
                .font(.system(size: titleSize, weight: .bold))
                .foregroundStyle(.white)
                .accessibilityAddTraits(.isHeader)
            Text("favorites.subtitle")
                .font(.system(size: subtitleSize))
                .foregroundStyle(favoritesSecondaryText)
        }
        .fixedSize(horizontal: false, vertical: true)
        .padding(.bottom, subtitleToList - cardSpacing)
    }

    private func track(_ id: String, at index: Int) {
        visible[id] = index
        save()
    }

    private func untrack(_ id: String) {
        visible[id] = nil
        save()
    }

    private func save() {
        guard restored, let first = visible.min(by: { $0.value < $1.value }) else { return }
        savedPosition = first.key
    }

    private func restore(_ proxy: ScrollViewProxy) {
        guard !restored, !state.favorites.isEmpty else { return }
        restored = true
        if state.favorites.contains(where: { $0.id == savedPosition }) {
            proxy.scrollTo(savedPosition, anchor: .top)
        }
    }
}

private struct CountBadge: View {
    let count: Int

    @ScaledMetric(relativeTo: .caption) private var size: CGFloat = 12

    var body: some View {
        Text("favorites.count \(count)")
            .font(.system(size: size, weight: .bold))
            .foregroundStyle(Theme.accent)
            .padding(.horizontal, badgePadding)
            .frame(minHeight: badgeHeight)
            .background(favoritesCardColor, in: Capsule())
    }
}
