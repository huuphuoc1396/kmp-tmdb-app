import SwiftUI
import TmdbCore

/// At least the 44pt touch target; the design's row is 34pt, so the screen trims 5pt of spacing above and below.
let tabsMinHeight: CGFloat = 44

struct CategoryTabs: View {
    let selected: TitleCategory
    let onSelect: (TitleCategory) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 29) {
                ForEach(TitleCategory.allCases, id: \.self) { category in
                    CategoryTab(label: category.label, selected: category == selected) {
                        onSelect(category)
                    }
                }
            }
            .padding(.leading, 33)
            .padding(.trailing, 16)
        }
    }
}

private struct CategoryTab: View {
    let label: LocalizedStringKey
    let selected: Bool
    let action: () -> Void

    @ScaledMetric(relativeTo: .body) private var labelSize: CGFloat = 16

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: labelSize, weight: selected ? .semibold : .regular))
                .foregroundStyle(.white.opacity(selected ? 1 : 0.4))
                .lineLimit(1)
                .padding(.horizontal, selected ? 14 : 0)
                .padding(.vertical, 6)
                .background(selected ? Theme.accent : .clear, in: Capsule())
                .frame(minHeight: tabsMinHeight)
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
