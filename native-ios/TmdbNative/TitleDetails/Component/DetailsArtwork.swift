import NukeUI
import SwiftUI

let artworkHeight: CGFloat = 493

/// Where the design's fade starts and becomes solid, as fractions of the artwork's height.
private let fadeStart = 260 / artworkHeight
private let fadeEnd = 412 / artworkHeight

private let fade = LinearGradient(
    stops: [
        .init(color: Theme.page.opacity(0), location: 0),
        .init(color: Theme.page.opacity(0), location: fadeStart),
        .init(color: Theme.page, location: fadeEnd),
        .init(color: Theme.page, location: 1),
    ],
    startPoint: .top,
    endPoint: .bottom
)

/// The decorative artwork at the top, fading into the page; a plain tile when there is no image.
struct DetailsArtwork: View {
    let url: URL?

    var body: some View {
        ZStack {
            Theme.track
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
