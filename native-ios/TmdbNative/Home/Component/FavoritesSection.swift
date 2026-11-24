import SwiftUI

private let cardColor = Color(red: 0x21 / 255, green: 0x1A / 255, blue: 0x30 / 255)
private let borderColor = Color(red: 0x31 / 255, green: 0x2A / 255, blue: 0x3F / 255)
private let badgeColor = Color(red: 0x38 / 255, green: 0x1A / 255, blue: 0x39 / 255)
private let secondaryText = Color(red: 0xA6 / 255, green: 0xA1 / 255, blue: 0xB5 / 255)

private let cardRadius: CGFloat = 16
private let cardPadding: CGFloat = 12
private let headerTop: CGFloat = 18
private let headerEnd: CGFloat = 4
private let titleToBadge: CGFloat = 8
private let badgeSize: CGFloat = 22
private let titleToSubtitle: CGFloat = 12
private let subtitleToRow: CGFloat = 16
private let itemSpacing: CGFloat = 12
private let itemWidth: CGFloat = 97
private let posterHeight: CGFloat = 147
private let posterRadius: CGFloat = 8
private let posterToName: CGFloat = 12
private let nameToMeta: CGFloat = 8
private let heartInset: CGFloat = 6
private let touchTarget: CGFloat = 48
private let bottomPadding: CGFloat = 14
private let lineHeightFactor: CGFloat = 1.25

struct FavoritesSection: View {
    let favorites: [FavoriteTitleItem]
    let count: Int
    @Binding var savedPosition: String
    let onIntent: (HomeIntent) -> Void

    @ScaledMetric(relativeTo: .title3) private var titleSize: CGFloat = 20
    @ScaledMetric(relativeTo: .caption) private var badgeTextSize: CGFloat = 12
    @ScaledMetric(relativeTo: .subheadline) private var viewAllSize: CGFloat = 14
    @ScaledMetric(relativeTo: .footnote) private var subtitleSize: CGFloat = 13
    @ScaledMetric(relativeTo: .subheadline) private var nameSize: CGFloat = 14
    @ScaledMetric(relativeTo: .caption) private var metaSize: CGFloat = 12

    private var rowHeight: CGFloat {
        let name: CGFloat = nameSize * lineHeightFactor * 2
        let meta: CGFloat = metaSize * lineHeightFactor
        let fixed: CGFloat = posterHeight + posterToName + nameToMeta
        return fixed + name + meta
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: titleToBadge) {
                Text("favorites.section.title")
                    .font(.system(size: titleSize, weight: .bold))
                    .foregroundStyle(.white)
                    .accessibilityAddTraits(.isHeader)
                Text("\(count)")
                    .font(.system(size: badgeTextSize, weight: .bold))
                    .foregroundStyle(Theme.accent)
                    .padding(.horizontal, 6)
                    .frame(minWidth: badgeSize, minHeight: badgeSize)
                    .background(badgeColor, in: Capsule())
                Spacer(minLength: 0)
                Button { onIntent(.seeAllFavoritesTapped) } label: {
                    HStack(spacing: 6) {
                        Text("favorites.section.viewAll")
                            .font(.system(size: viewAllSize, weight: .semibold))
                        Image("ChevronRight")
                            .resizable()
                            .frame(width: 12, height: 12)
                            .accessibilityHidden(true)
                    }
                    .foregroundStyle(Theme.accent)
                    .padding(.horizontal, 8)
                    .frame(minHeight: touchTarget)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
            .padding(.leading, cardPadding)
            .padding(.trailing, headerEnd)
            Text("favorites.section.subtitle")
                .font(.system(size: subtitleSize))
                .foregroundStyle(secondaryText)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal, cardPadding)
                .padding(.top, titleToSubtitle - (touchTarget - badgeSize) / 2)
            RestoringRow(
                items: favorites,
                itemWidth: itemWidth,
                spacing: itemSpacing,
                rowKey: "favorites",
                savedPosition: $savedPosition,
                leadingPadding: cardPadding
            ) { favorite in
                FavoriteItem(
                    favorite: favorite,
                    nameSize: nameSize,
                    metaSize: metaSize,
                    onTap: { onIntent(.favoriteTapped(favorite)) },
                    onRemoveTap: { onIntent(.removeFavoriteTapped(favorite)) }
                )
            }
            .frame(height: rowHeight)
            .padding(.top, subtitleToRow)
        }
        .padding(.top, headerTop - (touchTarget - badgeSize) / 2)
        .padding(.bottom, bottomPadding)
        .background(cardColor, in: RoundedRectangle(cornerRadius: cardRadius))
        .overlay(RoundedRectangle(cornerRadius: cardRadius).stroke(borderColor, lineWidth: 1))
    }
}

private struct FavoriteItem: View {
    let favorite: FavoriteTitleItem
    let nameSize: CGFloat
    let metaSize: CGFloat
    let onTap: () -> Void
    let onRemoveTap: () -> Void

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Button(action: onTap) {
                VStack(alignment: .leading, spacing: 0) {
                    PosterTile(name: favorite.name, posterURL: favorite.posterURL)
                        .frame(width: itemWidth, height: posterHeight)
                        .clipShape(RoundedRectangle(cornerRadius: posterRadius))
                    if let name = favorite.name {
                        Text(name)
                            .font(.system(size: nameSize, weight: .semibold))
                            .foregroundStyle(.white)
                            .lineLimit(2)
                            .padding(.top, posterToName)
                        if !favorite.meta.isEmpty {
                            Text(favorite.meta)
                                .font(.system(size: metaSize))
                                .foregroundStyle(secondaryText)
                                .lineLimit(1)
                                .padding(.top, nameToMeta)
                        }
                    }
                }
                .frame(width: itemWidth, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(favorite.name.map { Text($0) } ?? Text("favorites.loading"))
            .accessibilityAddTraits(.isButton)
            Button(action: onRemoveTap) {
                Image("FavoriteRemove")
                    .resizable()
                    .frame(width: 28, height: 28)
                    .frame(width: touchTarget, height: touchTarget)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .offset(x: (touchTarget - 28) / 2 - heartInset, y: heartInset - (touchTarget - 28) / 2)
            .accessibilityLabel(Text("removeFavorite.heart"))
        }
    }
}
