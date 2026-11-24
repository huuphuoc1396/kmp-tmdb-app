import NukeUI
import SwiftUI

private let itemWidth: CGFloat = 76
private let photoSize: CGFloat = 60
private let photoBorder: CGFloat = 4

struct CastRow: View {
    let cast: [CastItem]
    let rowKey: String
    @Binding var savedPosition: String

    @ScaledMetric(relativeTo: TmdbTextStyle.labelMedium.relativeTo)
    private var nameHeight = TmdbTextStyle.labelMedium.lineHeight * 2

    var body: some View {
        RestoringRow(
            items: cast.enumerated().map { CastEntry(id: "\($0.offset)-\($0.element.name)", member: $0.element) },
            itemWidth: itemWidth,
            spacing: TmdbSpacing.xxs,
            rowKey: rowKey,
            savedPosition: $savedPosition
        ) { entry in
            CastCell(member: entry.member, nameHeight: nameHeight)
        }
        .frame(height: photoSize + photoBorder * 2 + TmdbSpacing.xxs + nameHeight)
    }
}

private struct CastEntry: Identifiable {
    let id: String
    let member: CastItem
}

private struct CastCell: View {
    let member: CastItem
    let nameHeight: CGFloat

    var body: some View {
        VStack(spacing: TmdbSpacing.xxs) {
            photo
                .frame(width: photoSize, height: photoSize)
                .clipShape(Circle())
                .padding(photoBorder)
                .background(TmdbColor.surfaceContainerHigh, in: Circle())
            Text(verbatim: member.name)
                .tmdbFont(.labelMedium)
                .foregroundStyle(TmdbColor.onSurface)
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
            TmdbColor.surfaceContainerHigh
            if let url = member.photoURL {
                LazyImage(url: url) { state in
                    if let image = state.image {
                        image.resizable().aspectRatio(contentMode: .fill)
                    }
                }
            } else {
                Image(systemName: "person.fill")
                    .font(.system(size: 32))
                    .foregroundStyle(TmdbColor.onSurface.opacity(0.5))
            }
        }
        .accessibilityHidden(true)
    }
}
