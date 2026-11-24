import SwiftUI

struct FavoriteHeart: View {
    let isFavorite: Bool

    var body: some View {
        Image(isFavorite ? "HeartFilled" : "HeartOutline")
            .renderingMode(.template)
            .resizable()
            .scaledToFit()
            .frame(width: 16, height: 16)
            .foregroundStyle(isFavorite ? TmdbColor.secondary : TmdbColor.onSurface)
            .frame(width: 32, height: 32)
            .background(TmdbColor.surfaceContainerHigh.opacity(0.65), in: Circle())
    }
}
