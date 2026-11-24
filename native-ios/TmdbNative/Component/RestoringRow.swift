import SwiftUI

struct RestoringRow<Item: Identifiable, Cell: View>: View where Item.ID == String {
    let items: [Item]
    let itemWidth: CGFloat
    let spacing: CGFloat
    let rowKey: String
    @Binding var savedPosition: String
    var leadingPadding: CGFloat = homeScreenPadding
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
                    .padding(.leading, leadingPadding)
                    .padding(.trailing, TmdbSpacing.md)
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
        guard let index = items.firstIndex(where: { $0.id == id }), index > 0, rowWidth > itemWidth else { return }
        let anchor = UnitPoint(x: leadingPadding / (rowWidth - itemWidth), y: 0)
        DispatchQueue.main.async { proxy.scrollTo(id, anchor: anchor) }
    }

    private func savePosition() {
        guard restored, let first = visible.min(), items.indices.contains(first) else { return }
        savedPosition = positionPrefix + items[first].id
    }
}
