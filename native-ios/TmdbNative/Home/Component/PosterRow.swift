import NukeUI
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

    @ScaledMetric(relativeTo: .subheadline) private var nameSize: CGFloat = 14

    var body: some View {
        Button(action: action) {
            ZStack {
                Theme.surfaceVariant
                if let url = title.posterURL {
                    LazyImage(url: url) { state in
                        if let image = state.image {
                            image.resizable().aspectRatio(contentMode: .fill)
                        }
                    }
                } else {
                    Text(title.name)
                        .font(.system(size: nameSize, weight: .semibold))
                        .foregroundStyle(.white)
                        .multilineTextAlignment(.center)
                        .lineLimit(4)
                        .truncationMode(.tail)
                        .padding(8)
                }
            }
            .frame(width: posterWidth, height: posterHeight)
            .clipShape(posterShape)
            .contentShape(posterShape)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title.name)
    }
}
