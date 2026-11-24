import SwiftUI

private let posterWidth: CGFloat = 124
private let posterHeight: CGFloat = 188
private let posterShape = RoundedRectangle(cornerRadius: 8)

struct PosterRow: View {
    let titles: [TitleItem]
    let rowKey: String
    @Binding var savedPosition: String
    let onTap: (TitleItem) -> Void

    var body: some View {
        RestoringRow(
            items: titles,
            itemWidth: posterWidth,
            spacing: 12,
            rowKey: rowKey,
            savedPosition: $savedPosition
        ) { title in
            Poster(title: title) { onTap(title) }
        }
        .frame(height: posterHeight)
    }
}

private struct Poster: View {
    let title: TitleItem
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            PosterTile(name: title.name, posterURL: title.posterURL)
                .frame(width: posterWidth, height: posterHeight)
            .clipShape(posterShape)
            .contentShape(posterShape)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title.name)
    }
}
