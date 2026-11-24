import SwiftUI

struct RowMessage: View {
    let message: LocalizedStringKey
    var alignment: HorizontalAlignment = .leading
    var onRetry: (() -> Void)?

    var body: some View {
        VStack(alignment: alignment, spacing: TmdbSpacing.sm) {
            Text(message)
                .tmdbFont(.bodyLarge)
                .foregroundStyle(TmdbColor.onSurfaceVariant)
                .multilineTextAlignment(alignment == .center ? .center : .leading)
                .fixedSize(horizontal: false, vertical: true)
            if let onRetry {
                Button(action: onRetry) {
                    Text("home.retry")
                        .tmdbFont(.titleMedium)
                        .foregroundStyle(TmdbColor.onSecondary)
                        .padding(.horizontal, TmdbSpacing.md)
                        .padding(.vertical, TmdbSpacing.xs)
                        .background(TmdbColor.secondary, in: Capsule())
                        .frame(minHeight: TmdbSize.touchTarget)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .frame(
            maxWidth: .infinity,
            minHeight: PosterRowDefaults.posterHeight,
            alignment: Alignment(horizontal: alignment, vertical: .center)
        )
        .padding(.horizontal, TmdbSpacing.xxl)
    }
}
