import SwiftUI

/// The "Search" field, shown as designed with no action: not focusable, not tappable, skipped by screen readers.
struct SearchField: View {
    @ScaledMetric(relativeTo: .subheadline) private var hintSize: CGFloat = 15

    var body: some View {
        HStack {
            Text("home.searchHint")
                .font(.system(size: hintSize, weight: .medium))
                .foregroundStyle(.white.opacity(0.5))
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
            // The design mirrors the exported icon so the handle points down-left.
            Image(decorative: "SearchIcon")
                .scaleEffect(x: -1)
        }
        .padding(.leading, 21)
        .padding(.trailing, 16)
        .frame(minHeight: 46)
        .background(Theme.surfaceVariant, in: Capsule())
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}
