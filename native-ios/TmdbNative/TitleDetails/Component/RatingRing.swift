import SwiftUI

let ratingRingSize: CGFloat = 57
private let ringWidth: CGFloat = 6
/// Fixed, so the value never outgrows the fixed ring at large text sizes; VoiceOver still reads the label.
private let valueSize: CGFloat = 15

/// The TMDB score as a ring filled to `percent`, or an empty ring with "NR" when nobody has voted.
struct RatingRing: View {
    let percent: Int?

    var body: some View {
        ZStack {
            Circle().fill(Theme.track)
            Circle()
                .fill(Theme.ringCenter)
                .padding(ringWidth)
            if let percent {
                Circle()
                    .trim(from: 0, to: CGFloat(percent) / 100)
                    .stroke(Theme.accent, style: StrokeStyle(lineWidth: ringWidth, lineCap: .round))
                    .rotationEffect(.degrees(-90))
                    .padding(ringWidth / 2)
            }
            value
                .foregroundStyle(.white)
        }
        .frame(width: ratingRingSize, height: ratingRingSize)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityText)
    }

    @ViewBuilder
    private var value: some View {
        if let percent {
            Text(verbatim: String(percent)).font(.system(size: valueSize, weight: .bold))
                + Text(verbatim: "%").font(.system(size: valueSize))
        } else {
            Text("details.notRated")
                .font(.system(size: valueSize, weight: .bold))
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
    .background(Theme.page)
}
