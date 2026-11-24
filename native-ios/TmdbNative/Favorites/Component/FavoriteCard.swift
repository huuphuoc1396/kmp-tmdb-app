import SwiftUI

let favoritesCardColor = Color(red: 0x21 / 255, green: 0x1A / 255, blue: 0x30 / 255)
let favoritesSecondaryText = Color(red: 0xA6 / 255, green: 0xA1 / 255, blue: 0xB5 / 255)

private let cardHeight: CGFloat = 120
private let cardRadius: CGFloat = 12
private let cardPadding: CGFloat = 12
private let posterWidth: CGFloat = 64
private let posterHeight: CGFloat = 96
private let posterRadius: CGFloat = 6
private let posterToText: CGFloat = 14
private let nameToMeta: CGFloat = 8
private let heartSize = CGSize(width: 20, height: 18)
private let touchTarget: CGFloat = 48

struct FavoriteCard: View {
    let favorite: FavoriteTitleItem
    let onTap: () -> Void
    let onRemoveTap: () -> Void

    @ScaledMetric(relativeTo: .body) private var nameSize: CGFloat = 16
    @ScaledMetric(relativeTo: .footnote) private var metaSize: CGFloat = 13

    var body: some View {
        HStack(spacing: 0) {
            Button(action: onTap) {
                HStack(spacing: posterToText) {
                    PosterTile(name: favorite.name, posterURL: favorite.posterURL)
                        .frame(width: posterWidth, height: posterHeight)
                        .clipShape(RoundedRectangle(cornerRadius: posterRadius))
                    if let name = favorite.name {
                        VStack(alignment: .leading, spacing: nameToMeta) {
                            Text(name)
                                .font(.system(size: nameSize, weight: .semibold))
                                .foregroundStyle(.white)
                                .lineLimit(2)
                            if !favorite.meta.isEmpty {
                                Text(favorite.meta)
                                    .font(.system(size: metaSize))
                                    .foregroundStyle(favoritesSecondaryText)
                                    .lineLimit(1)
                            }
                        }
                    }
                    Spacer(minLength: 0)
                }
                .padding(.vertical, cardPadding)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(favorite.name.map { Text($0) } ?? Text("favorites.loading"))
            .accessibilityAddTraits(.isButton)
            Button(action: onRemoveTap) {
                Image("HeartFilled")
                    .resizable()
                    .frame(width: heartSize.width, height: heartSize.height)
                    .frame(width: touchTarget, height: touchTarget)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("removeFavorite.heart"))
        }
        .padding(.leading, cardPadding)
        .padding(.trailing, 10)
        .frame(minHeight: cardHeight)
        .background(favoritesCardColor, in: RoundedRectangle(cornerRadius: cardRadius))
    }
}
