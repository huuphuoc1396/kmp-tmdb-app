import NukeUI
import SwiftUI

private let itemWidth: CGFloat = 75
private let photoSize: CGFloat = 60
private let borderWidth: CGFloat = 4
private let photoToName: CGFloat = 3

struct CastRow: View {
    let cast: [CastItem]
    let rowKey: String
    @Binding var savedPosition: String

    @ScaledMetric(relativeTo: .caption) private var nameHeight: CGFloat = 31

    var body: some View {
        RestoringRow(
            items: cast.enumerated().map { CastEntry(id: "\($0.offset)-\($0.element.name)", member: $0.element) },
            itemWidth: itemWidth,
            spacing: 4,
            rowKey: rowKey,
            savedPosition: $savedPosition
        ) { entry in
            CastCell(member: entry.member, nameHeight: nameHeight)
        }
        .frame(height: photoSize + borderWidth * 2 + photoToName + nameHeight)
    }
}

private struct CastEntry: Identifiable {
    let id: String
    let member: CastItem
}

private struct CastCell: View {
    let member: CastItem
    let nameHeight: CGFloat

    @ScaledMetric(relativeTo: .caption) private var nameSize: CGFloat = 12

    var body: some View {
        VStack(spacing: photoToName) {
            photo
                .frame(width: photoSize, height: photoSize)
                .clipShape(Circle())
                .padding(borderWidth)
                .background(Theme.track, in: Circle())
            Text(verbatim: member.name)
                .font(.system(size: nameSize, weight: .bold))
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)
                .lineLimit(2)
                .truncationMode(.tail)
                .frame(width: itemWidth, height: nameHeight, alignment: .top)
        }
        .frame(width: itemWidth)
        .accessibilityElement(children: .combine)
    }

    @ViewBuilder
    private var photo: some View {
        ZStack {
            Theme.track
            if let url = member.photoURL {
                LazyImage(url: url) { state in
                    if let image = state.image {
                        image.resizable().aspectRatio(contentMode: .fill)
                    }
                }
            } else {
                Image(systemName: "person.fill")
                    .font(.system(size: 30))
                    .foregroundStyle(.white.opacity(0.5))
            }
        }
        .accessibilityHidden(true)
    }
}
