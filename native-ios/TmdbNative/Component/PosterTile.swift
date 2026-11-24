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
                NameText(name, style: .titleSmall, lineLimit: 4, alignment: .center)
                    .foregroundStyle(TmdbColor.onSurface)
                    .padding(TmdbSpacing.xs)
            }
        }
    }
}
