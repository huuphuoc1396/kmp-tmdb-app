import SwiftUI

/// Same height as a poster row, so the layout doesn't jump when a row has no posters.
let rowMinHeight: CGFloat = 188

struct RowMessage: View {
    let message: LocalizedStringKey
    var onRetry: (() -> Void)?

    @ScaledMetric(relativeTo: .subheadline) private var messageSize: CGFloat = 15
    @ScaledMetric(relativeTo: .body) private var retrySize: CGFloat = 16

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(message)
                .font(.system(size: messageSize))
                .foregroundStyle(.white.opacity(0.5))
                .fixedSize(horizontal: false, vertical: true)
            if let onRetry {
                Button(action: onRetry) {
                    Text("home.retry")
                        .font(.system(size: retrySize, weight: .semibold))
                        .foregroundStyle(.white)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 6)
                        .background(Theme.accent, in: Capsule())
                        .frame(minHeight: tabsMinHeight)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .frame(maxWidth: .infinity, minHeight: rowMinHeight, alignment: .leading)
        .padding(.horizontal, homeScreenPadding)
    }
}
