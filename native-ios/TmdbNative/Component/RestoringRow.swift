import SwiftUI

/// A horizontal row starting at the screen padding. It saves its first visible item in `savedPosition`
/// (`"<rowKey>|<item id>"`) and scrolls back to it the first time it shows, so the position survives the app being
/// killed. A new `rowKey` starts the row at the beginning.
struct RestoringRow<Item: Identifiable, Cell: View>: View where Item.ID == String {
    let items: [Item]
    let itemWidth: CGFloat
    let spacing: CGFloat
    let rowKey: String
    @Binding var savedPosition: String
    @ViewBuilder let cell: (Item) -> Cell

    @State private var visible: Set<Int> = []
    @State private var restored = false

    var body: some View {
        GeometryReader { geometry in
            ScrollViewReader { proxy in
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(alignment: .top, spacing: spacing) {
                        ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                            cell(item)
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
        .id(rowKey)
    }

    private var positionPrefix: String { "\(rowKey)|" }

    private func restorePosition(with proxy: ScrollViewProxy, rowWidth: CGFloat) {
        guard !restored else { return }
        restored = true
        guard savedPosition.hasPrefix(positionPrefix) else { return }
        let id = String(savedPosition.dropFirst(positionPrefix.count))
        // The first item is already where it belongs.
        guard let index = items.firstIndex(where: { $0.id == id }), index > 0, rowWidth > itemWidth else { return }
        // `scrollTo` lines up the same unit point of the item and the row, so this anchor puts the item at the
        // screen padding instead of the row's edge.
        let anchor = UnitPoint(x: homeScreenPadding / (rowWidth - itemWidth), y: 0)
        // After the first layout pass, so the lazy stack can find the item.
        DispatchQueue.main.async { proxy.scrollTo(id, anchor: anchor) }
    }

    private func savePosition() {
        // Nothing is visible while another screen covers the row; keep the last position then.
        guard restored, let first = visible.min(), items.indices.contains(first) else { return }
        savedPosition = positionPrefix + items[first].id
    }
}
