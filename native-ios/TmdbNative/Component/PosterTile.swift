import NukeUI
import SwiftUI

struct PosterTile: View {
    let name: String?
    let posterURL: URL?

    @ScaledMetric(relativeTo: .subheadline) private var nameSize: CGFloat = 14

    var body: some View {
        ZStack {
            Theme.surfaceVariant
            if let posterURL {
                LazyImage(url: posterURL) { state in
                    if let image = state.image {
                        image.resizable().aspectRatio(contentMode: .fill)
                    }
                }
            } else if let name {
                Text(name)
                    .font(.system(size: nameSize, weight: .semibold))
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
                    .lineLimit(4)
                    .truncationMode(.tail)
                    .padding(8)
            }
        }
    }
}
