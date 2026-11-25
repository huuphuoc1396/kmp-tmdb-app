import SwiftUI

struct FavoriteCard: View {
    let favorite: FavoriteTitleItem
    let onTap: () -> Void
    let onRemoveTap: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            Button(action: onTap) {
                HStack(spacing: TmdbSpacing.md) {
                    PosterTile(name: favorite.name, posterURL: favorite.posterURL)
                        .frame(width: 64, height: 96)
                        .clipShape(RoundedRectangle(cornerRadius: TmdbShape.small))
                    if let name = favorite.name {
                        VStack(alignment: .leading, spacing: TmdbSpacing.xs) {
                            NameText(name, style: .titleMedium, lineLimit: 2)
                                .foregroundStyle(TmdbColor.onSurface)
                            if !favorite.meta.isEmpty {
                                Text(favorite.meta)
                                    .tmdbFont(.bodyMedium)
                                    .foregroundStyle(TmdbColor.onSurfaceVariant)
                                    .lineLimit(1)
                            }
                        }
                    }
                    Spacer(minLength: 0)
                }
                .padding(.vertical, TmdbSpacing.sm)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(favorite.name.map { Text($0) } ?? Text("favorites.loading"))
            .accessibilityAddTraits(.isButton)
            Button(action: onRemoveTap) {
                FavoriteHeart(isFavorite: true)
                    .frame(width: TmdbSize.touchTarget, height: TmdbSize.touchTarget)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .accessibilityLabel(Text("removeFavorite.heart"))
        }
        .padding(.leading, TmdbSpacing.sm)
        .padding(.trailing, TmdbSpacing.sm)
        .frame(minHeight: 120)
        .background(TmdbColor.surfaceContainer, in: RoundedRectangle(cornerRadius: TmdbShape.medium))
    }
}
