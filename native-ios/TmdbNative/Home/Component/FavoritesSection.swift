import SwiftUI

private let badgeSize: CGFloat = 24
private let itemWidth: CGFloat = 96
private let posterHeight: CGFloat = 148
private let heartSize: CGFloat = 32

struct FavoritesSection: View {
    let favorites: [FavoriteTitleItem]
    let count: Int
    @Binding var savedPosition: String
    let onIntent: (HomeIntent) -> Void

    @ScaledMetric(relativeTo: TmdbTextStyle.titleSmall.relativeTo)
    private var nameLineHeight = TmdbTextStyle.titleSmall.lineHeight
    @ScaledMetric(relativeTo: TmdbTextStyle.bodySmall.relativeTo)
    private var metaLineHeight = TmdbTextStyle.bodySmall.lineHeight

    private var rowHeight: CGFloat {
        posterHeight + TmdbSpacing.sm + nameLineHeight * 2 + TmdbSpacing.xs + metaLineHeight
    }

    private var touchOffset: CGFloat {
        (TmdbSize.touchTarget - badgeSize) / 2
    }

    private var title: some View {
        Text("favorites.section.title")
            .tmdbFont(.titleLarge)
            .foregroundStyle(TmdbColor.onSurface)
            .accessibilityAddTraits(.isHeader)
    }

    private var badge: some View {
        Text("\(count)")
            .tmdbFont(.labelMedium)
            .foregroundStyle(TmdbColor.onSecondaryContainer)
            .padding(.horizontal, TmdbSpacing.xs)
            .frame(minWidth: badgeSize, minHeight: badgeSize)
            .background(TmdbColor.secondaryContainer, in: Capsule())
            .fixedSize()
    }

    private var viewAllButton: some View {
        Button { onIntent(.seeAllFavoritesTapped) } label: {
            HStack(spacing: TmdbSpacing.xs) {
                Text("favorites.section.viewAll")
                    .tmdbFont(.labelLarge)
                Image("ChevronRight")
                    .resizable()
                    .frame(width: 12, height: 12)
                    .accessibilityHidden(true)
            }
            .foregroundStyle(TmdbColor.secondary)
            .padding(.horizontal, TmdbSpacing.xs)
            .frame(minHeight: TmdbSize.touchTarget)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ViewThatFits(in: .horizontal) {
                HStack(spacing: TmdbSpacing.xs) {
                    title.fixedSize()
                    badge
                    Spacer(minLength: 0)
                    viewAllButton.fixedSize()
                }
                VStack(alignment: .leading, spacing: 0) {
                    HStack(spacing: TmdbSpacing.xs) {
                        title
                        badge
                    }
                    .frame(minHeight: TmdbSize.touchTarget)
                    viewAllButton
                        .fixedSize()
                        .frame(maxWidth: .infinity, alignment: .trailing)
                }
            }
            .padding(.leading, TmdbSpacing.sm)
            .padding(.trailing, TmdbSpacing.xxs)
            Text("favorites.section.subtitle")
                .tmdbFont(.bodyMedium)
                .foregroundStyle(TmdbColor.onSurfaceVariant)
                .fixedSize(horizontal: false, vertical: true)
                .padding(.horizontal, TmdbSpacing.sm)
                .padding(.top, TmdbSpacing.sm - touchOffset)
            RestoringRow(
                items: favorites,
                itemWidth: itemWidth,
                spacing: TmdbSpacing.sm,
                rowKey: "favorites",
                savedPosition: $savedPosition,
                leadingPadding: TmdbSpacing.sm
            ) { favorite in
                FavoriteItem(
                    favorite: favorite,
                    onTap: { onIntent(.favoriteTapped(favorite)) },
                    onRemoveTap: { onIntent(.removeFavoriteTapped(favorite)) }
                )
            }
            .frame(height: rowHeight)
            .padding(.top, TmdbSpacing.md)
        }
        .padding(.top, TmdbSpacing.lg - touchOffset)
        .padding(.bottom, TmdbSpacing.md)
        .background(TmdbColor.surfaceContainer, in: RoundedRectangle(cornerRadius: TmdbShape.large))
        .overlay(
            RoundedRectangle(cornerRadius: TmdbShape.large)
                .stroke(TmdbColor.surfaceContainerHigh, lineWidth: TmdbSize.borderWidth)
        )
    }
}

private struct FavoriteItem: View {
    let favorite: FavoriteTitleItem
    let onTap: () -> Void
    let onRemoveTap: () -> Void

    private var heartOffset: CGFloat {
        (TmdbSize.touchTarget - heartSize) / 2 - TmdbSpacing.xxs
    }

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Button(action: onTap) {
                VStack(alignment: .leading, spacing: 0) {
                    PosterTile(name: favorite.name, posterURL: favorite.posterURL)
                        .frame(width: itemWidth, height: posterHeight)
                        .clipShape(RoundedRectangle(cornerRadius: TmdbShape.small))
                    if let name = favorite.name {
                        NameText(name, style: .titleSmall, lineLimit: 2)
                            .foregroundStyle(TmdbColor.onSurface)
                            .padding(.top, TmdbSpacing.sm)
                        if !favorite.meta.isEmpty {
                            Text(favorite.meta)
                                .tmdbFont(.bodySmall)
                                .foregroundStyle(TmdbColor.onSurfaceVariant)
                                .lineLimit(1)
                                .padding(.top, TmdbSpacing.xs)
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
                FavoriteHeart(isFavorite: true)
                    .frame(width: TmdbSize.touchTarget, height: TmdbSize.touchTarget)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .offset(x: heartOffset, y: -heartOffset)
            .accessibilityLabel(Text("removeFavorite.heart"))
        }
    }
}
