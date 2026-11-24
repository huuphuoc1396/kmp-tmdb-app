import SwiftUI

private let ringWidth: CGFloat = 6

struct RatingRing: View {
    let percent: Int?

    var body: some View {
        ZStack {
            Circle().fill(TmdbColor.surfaceContainerHigh)
            Circle()
                .fill(TmdbColor.surface)
                .padding(ringWidth)
            if let percent {
                Circle()
                    .trim(from: 0, to: CGFloat(percent) / 100)
                    .stroke(TmdbColor.secondary, style: StrokeStyle(lineWidth: ringWidth, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .padding(ringWidth / 2)
            }
            value
                .tmdbFont(.titleMedium, weight: .regular)
                .foregroundStyle(TmdbColor.onSurface)
                .lineLimit(1)
                .minimumScaleFactor(0.5)
                .padding(TmdbSpacing.xs)
                .dynamicTypeSize(...DynamicTypeSize.xLarge)
        }
        .frame(width: 56, height: 56)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityText)
    }

    @ViewBuilder
    private var value: some View {
        if let percent {
            Text(verbatim: String(percent)).bold() + Text(verbatim: "%")
        } else {
            Text("details.notRated")
                .bold()
        }
    }

    private var accessibilityText: Text {
        if let percent {
            return Text("details.rating \(percent)")
        }
        return Text("details.notRatedDescription")
    }
}

#Preview {
    HStack {
        RatingRing(percent: 45)
        RatingRing(percent: 100)
        RatingRing(percent: nil)
    }
    .padding()
    .background(TmdbColor.surface)
}
