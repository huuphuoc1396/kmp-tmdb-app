import SwiftUI

struct GenreChips: View {
    let genres: [String]

    var body: some View {
        FlowLayout(spacing: TmdbSpacing.md, lineSpacing: TmdbSpacing.sm) {
            ForEach(Array(genres.enumerated()), id: \.offset) { _, genre in
                Text(verbatim: genre)
                    .tmdbFont(.titleMedium)
                    .foregroundStyle(TmdbColor.onSurface)
                    .lineLimit(1)
                    .padding(.horizontal, TmdbSpacing.md)
                    .frame(minHeight: 32)
                    .background(TmdbColor.surfaceContainerHigh, in: Capsule())
            }
        }
    }
}

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
        .background(TmdbColor.surface)
}
