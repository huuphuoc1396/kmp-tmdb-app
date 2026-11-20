import NukeUI
import SwiftUI
import TmdbCore

private let posterWidth: CGFloat = 124
private let posterHeight: CGFloat = 188
private let posterShape = RoundedRectangle(cornerRadius: 8)

/// A horizontal row of posters. It saves its first visible poster in `savedPosition` (`"<category>|<title id>"`)
/// and scrolls back to it the first time it shows that category, so the position survives the app being killed.
struct PosterRow: View {
    let titles: [TitleItem]
    let category: TitleCategory
    @Binding var savedPosition: String
    let onTap: (TitleItem) -> Void

    @State private var visible: Set<Int> = []
    @State private var restored = false

    var body: some View {
        GeometryReader { geometry in
            ScrollViewReader { proxy in
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(spacing: 12) {
                        ForEach(Array(titles.enumerated()), id: \.element.id) { index, title in
                            Poster(title: title) { onTap(title) }
                                .onAppear {
                                    visible.insert(index)
                                    savePosition()
                                }
                                .onDisappear {
                                    visible.remove(index)
                                    savePosition()
                                }
                        }
                    }
                    .padding(.leading, homeScreenPadding)
                    .padding(.trailing, 16)
                }
                .onAppear { restorePosition(with: proxy, rowWidth: geometry.size.width) }
            }
        }
        .frame(height: posterHeight)
        // A newly selected tab starts at the beginning.
        .id(category)
    }

    private var positionPrefix: String { "\(category.name)|" }

    private func restorePosition(with proxy: ScrollViewProxy, rowWidth: CGFloat) {
        guard !restored else { return }
        restored = true
        guard savedPosition.hasPrefix(positionPrefix) else { return }
        let id = String(savedPosition.dropFirst(positionPrefix.count))
        // The first poster is already where it belongs.
        guard let index = titles.firstIndex(where: { $0.id == id }), index > 0, rowWidth > posterWidth else { return }
        // `scrollTo` lines up the same unit point of the poster and the row, so this anchor puts the poster at the
        // screen padding instead of the row's edge.
        let anchor = UnitPoint(x: homeScreenPadding / (rowWidth - posterWidth), y: 0)
        // After the first layout pass, so the lazy stack can find the poster.
        DispatchQueue.main.async { proxy.scrollTo(id, anchor: anchor) }
    }

    private func savePosition() {
        // Nothing is visible while another screen covers Home; keep the last position then.
        guard restored, let first = visible.min(), titles.indices.contains(first) else { return }
        savedPosition = positionPrefix + titles[first].id
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
        // Announced once, as a button with the title's name, whether it shows an image or the placeholder.
        .accessibilityLabel(title.name)
    }
}
