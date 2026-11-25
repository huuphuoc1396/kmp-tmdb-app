import SwiftUI

struct DetailsTopBar: View {
    let isFavorite: Bool?
    let isScrolled: Bool
    let onBack: () -> Void
    let onFavoriteTap: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            BackButton(label: "details.back", isPill: true, action: onBack)
                .padding(.leading, TmdbSpacing.xs)
            Spacer(minLength: 0)
            if let isFavorite {
                FavoriteToggle(isOn: isFavorite, onTap: onFavoriteTap)
                    .padding(.trailing, TmdbSpacing.xl)
            }
        }
        .frame(minHeight: TmdbSize.touchTarget)
        .background {
            (isScrolled ? TmdbColor.surface : Color.clear)
                .ignoresSafeArea(edges: .top)
                .animation(.default, value: isScrolled)
        }
    }
}
