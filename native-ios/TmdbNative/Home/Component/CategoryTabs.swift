import SwiftUI
import TmdbCore

struct CategoryTabs: View {
    let selected: TitleCategory
    let onSelect: (TitleCategory) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: TmdbSpacing.xxl) {
                ForEach(TitleCategory.allCases, id: \.self) { category in
                    CategoryTab(label: category.label, selected: category == selected) {
                        onSelect(category)
                    }
                }
            }
            .padding(.leading, TmdbSpacing.xxl)
            .padding(.trailing, TmdbSpacing.md)
        }
    }
}

private struct CategoryTab: View {
    let label: LocalizedStringKey
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(label)
                .tmdbFont(selected ? .titleMedium : .bodyLarge)
                .foregroundStyle(selected ? TmdbColor.onSecondary : TmdbColor.onSurfaceVariant)
                .lineLimit(1)
                .padding(.horizontal, selected ? TmdbSpacing.md : 0)
                .padding(.vertical, TmdbSpacing.xs)
                .background(selected ? TmdbColor.secondary : .clear, in: Capsule())
                .frame(minHeight: TmdbSize.touchTarget)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(selected ? .isSelected : [])
    }
}

private extension TitleCategory {
    var label: LocalizedStringKey {
        switch self {
        case .movies: "home.category.movies"
        case .series: "home.category.series"
        case .anime: "home.category.anime"
        case .novelas: "home.category.novelas"
        case .documentaries: "home.category.documentaries"
        }
    }
}
