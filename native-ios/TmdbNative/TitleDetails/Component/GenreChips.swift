import SwiftUI

/// One chip per genre, as wide as its name, wrapping onto more lines; not tappable.
struct GenreChips: View {
    let genres: [String]

    @ScaledMetric(relativeTo: .subheadline) private var labelSize: CGFloat = 15

    var body: some View {
        FlowLayout(spacing: 14, lineSpacing: 10) {
            ForEach(Array(genres.enumerated()), id: \.offset) { _, genre in
                Text(verbatim: genre)
                    .font(.system(size: labelSize, weight: .medium))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                    .padding(.horizontal, 17.5)
                    .frame(minHeight: 31)
                    .background(Theme.track, in: Capsule())
            }
        }
    }
}

/// Places its views left to right and starts a new line when the next one doesn't fit.
private struct FlowLayout: Layout {
    let spacing: CGFloat
    let lineSpacing: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache _: inout ()) -> CGSize {
        let width = proposal.width ?? .infinity
        let frames = layout(subviews, width: width)
        let usedWidth = frames.map(\.maxX).max() ?? 0
        let height = frames.map(\.maxY).max() ?? 0
        return CGSize(width: proposal.width ?? usedWidth, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal _: ProposedViewSize, subviews: Subviews, cache _: inout ()) {
        for (subview, frame) in zip(subviews, layout(subviews, width: bounds.width)) {
            subview.place(
                at: CGPoint(x: bounds.minX + frame.minX, y: bounds.minY + frame.minY),
                proposal: ProposedViewSize(frame.size)
            )
        }
    }

    private func layout(_ subviews: Subviews, width: CGFloat) -> [CGRect] {
        var frames: [CGRect] = []
        var x: CGFloat = 0
        var y: CGFloat = 0
        var lineHeight: CGFloat = 0
        for subview in subviews {
            // A chip wider than the line is limited to it, so its name is cut instead of overflowing.
            var size = subview.sizeThatFits(.unspecified)
            size.width = min(size.width, width)
            if x > 0, x + size.width > width {
                x = 0
                y += lineHeight + lineSpacing
                lineHeight = 0
            }
            frames.append(CGRect(origin: CGPoint(x: x, y: y), size: size))
            x += size.width + spacing
            lineHeight = max(lineHeight, size.height)
        }
        return frames
    }
}

#Preview {
    GenreChips(genres: ["Drama", "Crime", "Thriller", "Science Fiction", "Action & Adventure", "War"])
        .frame(width: 315)
        .padding()
        .background(Theme.page)
}
