import SwiftUI

/// The design's 148.66° gradient runs from the top-left to the bottom-right of the pill.
private let gradient = LinearGradient(
    colors: [Theme.primary, Theme.primaryContainer],
    startPoint: .topLeading,
    endPoint: .bottomTrailing
)

/// The full-width "Assistir trailer" pill.
struct TrailerButton: View {
    let action: () -> Void

    @ScaledMetric(relativeTo: .body) private var labelSize: CGFloat = 16
    @ScaledMetric(relativeTo: .body) private var iconSize: CGFloat = 20

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image("PlayCircle")
                    .resizable()
                    .frame(width: iconSize, height: iconSize)
                    .accessibilityHidden(true)
                Text("details.watchTrailer")
                    .font(.system(size: labelSize, weight: .semibold))
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.center)
            }
            .padding(12)
            .frame(maxWidth: .infinity, minHeight: 44)
            .background(gradient, in: Capsule())
            .contentShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}
