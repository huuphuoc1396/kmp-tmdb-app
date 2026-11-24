import NukeUI
import SwiftUI

private let artworkHeight: CGFloat = 492

private let fadeStart = 260 / artworkHeight
private let fadeEnd = 412 / artworkHeight

private let fade = LinearGradient(
    stops: [
        .init(color: TmdbColor.surface.opacity(0), location: 0),
        .init(color: TmdbColor.surface.opacity(0), location: fadeStart),
        .init(color: TmdbColor.surface, location: fadeEnd),
        .init(color: TmdbColor.surface, location: 1),
    ],
    startPoint: .top,
    endPoint: .bottom
)

struct DetailsArtwork: View {
    let url: URL?

    var body: some View {
        ZStack {
            TmdbColor.surfaceContainerHigh
            if let url {
                LazyImage(url: url) { state in
                    if let image = state.image {
                        image.resizable().aspectRatio(contentMode: .fill)
                    }
                }
            }
            fade
        }
        .frame(maxWidth: .infinity)
        .frame(height: artworkHeight)
        .clipped()
        .accessibilityHidden(true)
    }
}
