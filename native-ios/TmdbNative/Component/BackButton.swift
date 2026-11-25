import SwiftUI

private let pillHeight: CGFloat = 32

struct BackButton: View {
    let label: LocalizedStringKey
    var isPill = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: TmdbSpacing.xs) {
                Image("BackArrow")
                    .resizable()
                    .frame(width: TmdbSize.iconMedium, height: TmdbSize.iconMedium)
                    .accessibilityHidden(true)
                Text(label)
                    .tmdbFont(.titleMedium)
                    .lineLimit(1)
                    .fixedSize()
            }
            .foregroundStyle(TmdbColor.onSurface)
            .padding(.horizontal, isPill ? TmdbSpacing.sm : 0)
            .frame(minHeight: isPill ? pillHeight : TmdbSize.touchTarget)
            .background(isPill ? TmdbColor.surfaceContainerHigh.opacity(0.65) : .clear, in: Capsule())
            .frame(minHeight: TmdbSize.touchTarget)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}
