import NukeUI
import SwiftUI

struct PosterTile: View {
    let name: String?
    let posterURL: URL?

    var body: some View {
        ZStack {
            TmdbColor.surfaceVariant
            if let posterURL {
                LazyImage(url: posterURL) { state in
                    if let image = state.image {
                        image.resizable().aspectRatio(contentMode: .fill)
                    }
                }
            } else if let name {
                Text(name)
                    .tmdbFont(.titleSmall)
                    .foregroundStyle(TmdbColor.onSurface)
                    .multilineTextAlignment(.center)
                    .lineLimit(4)
                    .truncationMode(.tail)
                    .padding(TmdbSpacing.xs)
            }
        }
    }
}
