import SwiftUI
import TmdbCore

struct FavoritesView: View {
    @StateObject private var viewModel: FavoritesViewModel
    private let onTitleTap: (TitleItem) -> Void

    @SceneStorage("favorites.scroll") private var savedPosition = ""
    @Environment(\.dismiss) private var dismiss

    init(favoriteTitles: any FavoriteTitles, favorites: any Favorites, onTitleTap: @escaping (TitleItem) -> Void) {
        _viewModel = StateObject(
            wrappedValue: FavoritesViewModel(favoriteTitles: favoriteTitles, favorites: favorites)
        )
        self.onTitleTap = onTitleTap
    }

    var body: some View {
        FavoritesContent(
            state: viewModel.state,
            onIntent: viewModel.onIntent,
            onBack: { dismiss() },
            savedPosition: $savedPosition
        )
        .toolbar(.hidden, for: .navigationBar)
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
    let onBack: () -> Void
    @Binding var savedPosition: String

    @State private var visible: [String: Int] = [:]
    @State private var restored = false

    var body: some View {
        VStack(spacing: 0) {
            FavoritesTopBar(count: state.favorites.count, onBack: onBack)
            if state.favorites.isEmpty {
                emptyContent
            } else {
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: TmdbSpacing.sm) {
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
                        }
                        .padding(.horizontal, TmdbSpacing.lg)
                        .padding(.top, TmdbSpacing.md)
                        .padding(.bottom, TmdbSpacing.xl)
                    }
                    .onAppear { restore(proxy) }
                }
            }
        }
        .background(TmdbColor.surface.ignoresSafeArea())
        .removeFavoriteDialog(
            isPresented: state.pendingRemoval != nil,
            titleName: state.pendingRemoval?.name,
            onConfirm: { onIntent(.removeConfirmed) },
            onDismiss: { onIntent(.removeDismissed) }
        )
    }

    private var emptyContent: some View {
        VStack(alignment: .leading, spacing: 0) {
            header
            Spacer(minLength: TmdbSpacing.xl)
            Text("favorites.empty")
                .tmdbFont(.bodyMedium)
                .foregroundStyle(TmdbColor.onSurfaceVariant)
                .multilineTextAlignment(.center)
                .frame(maxWidth: .infinity)
            Spacer(minLength: TmdbSpacing.xl)
        }
        .padding(.horizontal, TmdbSpacing.lg)
        .padding(.top, TmdbSpacing.md)
        .padding(.bottom, TmdbSpacing.xl)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: TmdbSpacing.xs) {
            Text("favorites.title")
                .tmdbFont(.headlineMedium)
                .foregroundStyle(TmdbColor.onSurface)
                .accessibilityAddTraits(.isHeader)
            Text("favorites.subtitle")
                .tmdbFont(.bodyMedium)
                .foregroundStyle(TmdbColor.onSurfaceVariant)
        }
        .fixedSize(horizontal: false, vertical: true)
        .padding(.bottom, TmdbSpacing.sm)
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

private struct FavoritesTopBar: View {
    let count: Int
    let onBack: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            BackButton(label: "favorites.back", action: onBack)
            Spacer(minLength: TmdbSpacing.xs)
            CountBadge(count: count)
        }
        .frame(minHeight: TmdbSize.touchTarget)
        .padding(.horizontal, TmdbSpacing.lg)
        .padding(.top, TmdbSpacing.xs)
    }
}

private struct CountBadge: View {
    let count: Int

    var body: some View {
        Text("favorites.count \(count)")
            .tmdbFont(.labelMedium)
            .foregroundStyle(TmdbColor.secondary)
            .padding(.horizontal, TmdbSpacing.sm)
            .frame(minHeight: 28)
            .background(TmdbColor.surfaceContainer, in: Capsule())
    }
}
