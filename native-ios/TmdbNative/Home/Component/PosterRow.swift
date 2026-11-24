import SwiftUI

private let posterShape = RoundedRectangle(cornerRadius: TmdbShape.small)

struct PosterRow: View {
    let titles: [TitleItem]
    let rowKey: String
    @Binding var savedPosition: String
    let onTap: (TitleItem) -> Void

    var body: some View {
        RestoringRow(
            items: titles,
            itemWidth: PosterRowDefaults.posterWidth,
            spacing: TmdbSpacing.sm,
            rowKey: rowKey,
            savedPosition: $savedPosition
        ) { title in
            Poster(title: title) { onTap(title) }
        }
        .frame(height: PosterRowDefaults.posterHeight)
    }
}

private struct Poster: View {
    let title: TitleItem
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            PosterTile(name: title.name, posterURL: title.posterURL)
                .frame(width: PosterRowDefaults.posterWidth, height: PosterRowDefaults.posterHeight)
            .clipShape(posterShape)
            .contentShape(posterShape)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title.name)
    }
}

enum PosterRowDefaults {
    static let posterWidth: CGFloat = 124
    static let posterHeight: CGFloat = 188
}
